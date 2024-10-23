package net.dodian.uber.comm;

import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SocketHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SocketHandler.class);
    private static final int BUFFER_SIZE = 8192;
    private static final long SELECTOR_TIMEOUT = 10;
    private static final int MAX_WRITES_PER_CYCLE = 10;

    private final Client player;
    private final SocketChannel socketChannel;
    private final AtomicBoolean processRunning = new AtomicBoolean(true);
    private final Queue<PacketData> incomingPackets = new ConcurrentLinkedQueue<>();
    private final Queue<ByteBuffer> outData = new ConcurrentLinkedQueue<>();
    private final ReentrantLock outputLock = new ReentrantLock();
    private final Condition cleanupDone = outputLock.newCondition();

    private ByteBuffer inputBuffer;
    private Selector selector;
    private PacketParser packetParser;

    public SocketHandler(Client player, SocketChannel socketChannel) throws IOException {
        this.player = player;
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
        this.selector = Selector.open();
        this.socketChannel.register(selector, SelectionKey.OP_READ);
        this.inputBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.packetParser = new PacketParser();
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

                if (!outData.isEmpty()) {
                    writeOutput();
                }

                player.timeOutCounter = 0;
            }
        } catch (IOException e) {
            logger.error("SocketHandler: Error in run", e);
        } finally {
            cleanup(); // Ensure cleanup even on exceptions
        }
    }

    private boolean isConnected() {
        return socketChannel != null && socketChannel.isOpen() && socketChannel.isConnected() && !player.disconnected;
    }

    public void logout() {
        if (processRunning.getAndSet(false)) {
            player.disconnected = true;
            cleanup();
        }
    }

    private void cleanup() {
        outputLock.lock();
        try {
            flushRemainingData();
            if (selector != null) {
                selector.close();
            }
            if (socketChannel != null) {
                socketChannel.close();
            }
            logger.info("{} has disconnected.", player.getPlayerName());

            // Signal cleanup completion
            cleanupDone.signalAll();
        } catch (IOException e) {
            logger.warn("SocketHandler Cleanup Exception", e);
        } finally {
            outputLock.unlock();
            player.disconnected = true; // Ensure disconnection
        }
    }

    private void flushRemainingData() {
        outputLock.lock();
        try {
            ByteBuffer buffer;
            while ((buffer = outData.poll()) != null) {
                while (buffer.hasRemaining()) {
                    socketChannel.write(buffer);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to flush remaining data", e);
        } finally {
            outputLock.unlock();
        }
    }

    private void parsePackets() throws IOException {
        inputBuffer.clear();
        int bytesRead = socketChannel.read(inputBuffer);
        if (bytesRead == -1) {
            throw new IOException("Client disconnected");
        }

        inputBuffer.flip();
        packetParser.parsePackets(inputBuffer, player, incomingPackets);
        inputBuffer.compact();
    }

    private void writeOutput() throws IOException {
        outputLock.lock();
        try {
            int writesThisCycle = 0;
            ByteBuffer buffer;
            while ((buffer = outData.peek()) != null && writesThisCycle < MAX_WRITES_PER_CYCLE) {
                int bytesWritten = socketChannel.write(buffer);
                if (bytesWritten == 0) break;

                if (!buffer.hasRemaining()) {
                    outData.poll();
                    writesThisCycle++;
                } else {
                    break;
                }
            }

            updateInterestOps();
        } finally {
            outputLock.unlock();
        }
    }

    private void updateInterestOps() {
        SelectionKey key = socketChannel.keyFor(selector);
        if (key != null && key.isValid()) {
            int ops = SelectionKey.OP_READ;
            if (!outData.isEmpty()) {
                ops |= SelectionKey.OP_WRITE;
            }
            key.interestOps(ops);
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

        outputLock.lock();
        try {
            ByteBuffer buffer = ByteBuffer.allocate(length);
            buffer.put(data, offset, length);
            buffer.flip();
            outData.offer(buffer);

            updateInterestOps();
        } finally {
            outputLock.unlock();
        }
    }

    public void awaitCleanup() throws InterruptedException {
        outputLock.lock();
        try {
            while (processRunning.get()) {
                cleanupDone.await(); // Wait for cleanup signal
            }
        } finally {
            outputLock.unlock();
        }
    }
}
