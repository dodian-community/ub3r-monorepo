package net.dodian.uber.comm;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Client;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class SocketHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(SocketHandler.class.getName());
    private static final int BUFFER_SIZE = 16384;
    private static final long SELECTOR_TIMEOUT = 10; // Reduced to 10ms for more frequent checks
    private static final int MAX_BUFFERS = 1000;

    private final Client player;
    private final SocketChannel socketChannel;
    private final AtomicBoolean processRunning = new AtomicBoolean(true);
    private final Queue<PacketData> incomingPackets = new ConcurrentLinkedQueue<>();
    private final Deque<ByteBuffer> outData = new ArrayDeque<>();
    private final ArrayDeque<ByteBuffer> bufferPool = new ArrayDeque<>(MAX_BUFFERS);

    private ByteBuffer inputBuffer;
    private ByteBuffer packetBuffer;
    private Selector selector;
    private int packetSize = -1;
    private int packetType = -1;

    public SocketHandler(Client player, SocketChannel socketChannel) throws IOException {
        this.player = player;
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
        this.selector = Selector.open();
        this.socketChannel.register(selector, SelectionKey.OP_READ);
        initializeBuffers();
        initializeBufferPool();
    }

    private void initializeBuffers() {
        this.inputBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        this.packetBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    }

    private void initializeBufferPool() {
        for (int i = 0; i < MAX_BUFFERS; i++) {
            bufferPool.offerLast(ByteBuffer.allocateDirect(BUFFER_SIZE));
        }
    }

    private ByteBuffer getBufferFromPool() {
        ByteBuffer buffer = bufferPool.pollLast();
        return (buffer != null) ? buffer : ByteBuffer.allocateDirect(BUFFER_SIZE);
    }

    private void returnBufferToPool(ByteBuffer buffer) {
        if (bufferPool.size() < MAX_BUFFERS) {
            buffer.clear();
            bufferPool.offerLast(buffer);
        }
    }

    @Override
    public void run() {
        try {
            while (processRunning.get() && isConnected()) {
                int readyOps = selector.select(SELECTOR_TIMEOUT);
                if (readyOps > 0) {
                    for (SelectionKey key : selector.selectedKeys()) {
                        if (!key.isValid()) continue;
                        if (key.isReadable()) {
                            parsePackets();
                        }
                        if (key.isWritable()) {
                            writeOutput();
                        }
                    }
                    selector.selectedKeys().clear();
                }

                // Check for write operations even if selector didn't return any
                if (!outData.isEmpty()) {
                    writeOutput();
                }

                player.timeOutCounter = 0;
            }
        } catch (EOFException e) {
            logger.info("Client disconnected: " + player.getPlayerName());
        } catch (IOException e) {
            logger.severe("SocketHandler: Error in run: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private boolean isConnected() {
        return socketChannel != null && socketChannel.isOpen() && socketChannel.isConnected() && !player.disconnected;
    }

    public void logout() {
        if (processRunning.getAndSet(false)) {
            player.disconnected = true;
            cleanup();
            YellSystem.alertStaff(player.getPlayerName() + " has logged out correctly!");
        }
    }

    private void cleanup() {
        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
            player.disconnected = true;
            YellSystem.alertStaff(player.getPlayerName() + " has disconnected.");
        } catch (IOException e) {
            logger.warning("SocketHandler Cleanup Exception: " + e.getMessage());
        }
    }

    private void parsePackets() throws IOException {
        inputBuffer.clear();
        int bytesRead = socketChannel.read(inputBuffer);
        if (bytesRead == -1) {
            throw new EOFException("Client disconnected");
        }

        inputBuffer.flip();
        while (inputBuffer.hasRemaining()) {
            if (packetType == -1) {
                if (!inputBuffer.hasRemaining()) break;
                packetType = inputBuffer.get() & 0xff;
                packetType = player.inStreamDecryption != null
                        ? (packetType - player.inStreamDecryption.getNextKey() & 0xff)
                        : packetType;
                packetSize = getPacketSize(packetType);
                switch (packetSize) {
                    case -1:
                        if (!inputBuffer.hasRemaining()) return;
                        packetSize = inputBuffer.get() & 0xff;
                        break;
                    case -2:
                        if (inputBuffer.remaining() < 2) return;
                        packetSize = inputBuffer.getShort() & 0xffff;
                        break;
                }
            }

            if (inputBuffer.remaining() < packetSize) break;

            packetBuffer.clear();
            for (int i = 0; i < packetSize; i++) {
                packetBuffer.put(inputBuffer.get());
            }

            packetBuffer.flip();
            byte[] data = new byte[packetSize];
            packetBuffer.get(data);
            incomingPackets.offer(new PacketData(packetType, data, packetSize));
            packetType = -1;
            packetSize = -1;
        }

        inputBuffer.compact();
    }

    private int getPacketSize(int packetType) {
        if (packetType < 0 || packetType >= Constants.PACKET_SIZES.length) {
            return -1; // Invalid packet type
        }
        return Constants.PACKET_SIZES[packetType];
    }

    public Queue<PacketData> getPackets() {
        return incomingPackets;
    }

    public void queueOutput(byte[] data, int offset, int length) {
        if (!isConnected()) {
            logger.warning("Attempted to queue output for disconnected player: " + player.getPlayerName());
            return;
        }

        ByteBuffer buffer = getBufferFromPool();
        buffer.put(data, offset, length);
        buffer.flip();
        outData.offerLast(buffer);

        SelectionKey key = socketChannel.keyFor(selector);
        if (key != null && (key.interestOps() & SelectionKey.OP_WRITE) == 0) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    private void writeOutput() throws IOException {
        ByteBuffer buffer;
        while ((buffer = outData.peek()) != null) {
            int bytesWritten = socketChannel.write(buffer);
            if (bytesWritten == 0) {

                break;
            }

            if (!buffer.hasRemaining()) {
                outData.poll();
                returnBufferToPool(buffer);
            } else {

                break;
            }
        }

        SelectionKey key = socketChannel.keyFor(selector);
        if (key != null) {
            if (outData.isEmpty()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            } else {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
        }
    }
}