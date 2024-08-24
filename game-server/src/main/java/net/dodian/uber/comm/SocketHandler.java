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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.ArrayDeque;



public class SocketHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(SocketHandler.class.getName());
    private static final int BUFFER_SIZE = 8000;
    private static final long READ_TIMEOUT = 5000; // 5 seconds
    private static final int MAX_BUFFERS = 100;

    private final Client player;
    private final SocketChannel socketChannel;
    private final AtomicBoolean processRunning = new AtomicBoolean(true);
    private final PacketQueue myPackets = new PacketQueue();
    private final Queue<ByteBuffer> outData = new ConcurrentLinkedQueue<>();
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
        buffer.clear();
        if (bufferPool.size() < MAX_BUFFERS) {
            bufferPool.offerLast(buffer);
        }
    }

    @Override
    public void run() {
        try {
            while (processRunning.get() && isConnected()) {
                if (selector.select(READ_TIMEOUT) > 0) {
                    for (SelectionKey key : selector.selectedKeys()) {
                        if (!key.isValid()) {
                            continue;
                        }
                        if (key.isReadable()) {
                            parsePackets();
                        }
                        if (key.isWritable()) {
                            writeOutput();
                        }
                    }
                    selector.selectedKeys().clear();
                } else {

                    if (player.disconnected) {
                        break;
                    }

                    logger.warning("Read timeout for player: " + player.getPlayerName());
                    break;
                }

                if (!outData.isEmpty() && !player.disconnected) {
                    SelectionKey key = socketChannel.keyFor(selector);
                    if (key == null || (key.interestOps() & SelectionKey.OP_WRITE) == 0) {
                        socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    }
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
            PacketData pData = new PacketData(packetType, data, packetSize);
            myPackets.add(pData); // Updated to use PacketQueue
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

        return myPackets.getPackets();
    }


    public synchronized void queueOutput(byte[] copy) {
        if (!isConnected()) {
            logger.warning("Attempted to queue output for disconnected player: " + player.getPlayerName());
            return;
        }
        ByteBuffer buffer = getBufferFromPool();
        buffer.put(copy);
        buffer.flip();
        outData.add(buffer);
        try {
            SelectionKey key = socketChannel.keyFor(selector);
            if (key == null || (key.interestOps() & SelectionKey.OP_WRITE) == 0) {
                socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
        } catch (IOException e) {
            logger.warning("SocketHandler: Error registering write operation: " + e.getMessage());
            cleanup();
        }
    }



    private void writeOutput() throws IOException {
        ByteBuffer buffer;
        while ((buffer = outData.peek()) != null) {
            socketChannel.write(buffer);
            if (!buffer.hasRemaining()) {
                outData.poll();
                returnBufferToPool(buffer); // Reuse the buffer
            } else {
                break;
            }
        }
        if (outData.isEmpty()) {
            socketChannel.register(selector, SelectionKey.OP_READ);
        }
    }
}
//packet