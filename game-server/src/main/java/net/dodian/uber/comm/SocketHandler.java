package net.dodian.uber.comm;

import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SocketHandler.class);
    public static final int BUFFER_SIZE = 8192; // 8KB buffer size
    private static final long SELECTOR_TIMEOUT = 10; // 10ms for more frequent checks
    private static final int MAX_BUFFERS = 1000;
    private static final int MAX_WRITES_PER_CYCLE = 10; // Limit writes per cycle

    private final Client player;
    private final SocketChannel socketChannel;
    private final AtomicBoolean processRunning = new AtomicBoolean(true);
    private final Queue<PacketData> incomingPackets = new ConcurrentLinkedQueue<>();
    private final Deque<ByteBuffer> outData = new ArrayDeque<>();
    private final ArrayDeque<ByteBuffer> bufferPool = new ArrayDeque<>(MAX_BUFFERS);

    private ByteBuffer inputBuffer;
    private Selector selector;
    private PacketParser packetParser;

    public SocketHandler(Client player, SocketChannel socketChannel) throws IOException {
        this.player = player;
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
        this.selector = Selector.open();
        this.socketChannel.register(selector, SelectionKey.OP_READ);
        initializeBuffers();
        initializeBufferPool();
        this.packetParser = new PacketParser();
    }

    private void initializeBuffers() {
        this.inputBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
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
            logger.info("Client disconnected: {}", player.getPlayerName());
        } catch (IOException e) {
            logger.error("SocketHandler: Error in run", e);
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
            // Attempt to flush any remaining data
            flushRemainingData();

            if (selector != null && selector.isOpen()) {
                selector.close();
            }
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
            player.disconnected = true;
            logger.info("{} has disconnected.", player.getPlayerName());
        } catch (IOException e) {
            logger.warn("SocketHandler Cleanup Exception", e);
        }
    }

    private void flushRemainingData() {
        try {
            ByteBuffer buffer;
            while ((buffer = outData.poll()) != null) {
                while (buffer.hasRemaining()) {
                    int written = socketChannel.write(buffer);
                    if (written == 0) {
                        // Buffer is full, try again later
                        Thread.sleep(1);
                    }
                }
                returnBufferToPool(buffer);
            }
        } catch (IOException e) {
            logger.warn("Failed to flush remaining data", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while flushing remaining data", e);
        }
    }

    private void parsePackets() throws IOException {
        inputBuffer.clear();
        int bytesRead = socketChannel.read(inputBuffer);
        if (bytesRead == -1) {
            throw new EOFException("Client disconnected");
        }

        inputBuffer.flip();
        packetParser.parsePackets(inputBuffer, player, incomingPackets);
        inputBuffer.compact();
    }

    private void writeOutput() throws IOException {
        int writesThisCycle = 0;
        ByteBuffer buffer;
        while ((buffer = outData.peek()) != null && writesThisCycle < MAX_WRITES_PER_CYCLE) {
            int bytesWritten = socketChannel.write(buffer);
            if (bytesWritten == 0) {

                break;
            }

            if (!buffer.hasRemaining()) {
                outData.poll();
                returnBufferToPool(buffer);
                writesThisCycle++;
            } else {

                break;
            }
        }

        // Update the interest ops based on whether we have more data to write
        SelectionKey key = socketChannel.keyFor(selector);
        if (key != null && key.isValid()) {
            if (outData.isEmpty()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            } else {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            }
        }
    }

    public Queue<PacketData> getPackets() {
        return incomingPackets;
    }

    public void queueOutput(byte[] data, int offset, int length) {
        if (!isConnected()) {
            logger.warn("Attempted to queue output for disconnected player: {}", player.getPlayerName());
            return;
        }

        ByteBuffer buffer = getBufferFromPool();
        buffer.put(data, offset, length);
        buffer.flip();
        outData.offerLast(buffer);

        SelectionKey key = socketChannel.keyFor(selector);
        if (key != null && key.isValid() && (key.interestOps() & SelectionKey.OP_WRITE) == 0) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }
}