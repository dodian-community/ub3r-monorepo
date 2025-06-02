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
                try {
                    int readyOps = selector.select(SELECTOR_TIMEOUT);
                    if (readyOps > 0) {
                        for (SelectionKey key : selector.selectedKeys()) {
                            if (!key.isValid()) {
                                key.cancel();
                                continue;
                            }
                            try {
                                if (key.isReadable()) {
                                    parsePackets();
                                }
                                if (key.isWritable()) {
                                    writeOutput();
                                }
                            } catch (IOException e) {
                                if (processRunning.get()) {  // Only log if we weren't already shutting down
                                    if (e.getMessage() != null &&
                                        (e.getMessage().contains("reset") ||
                                         e.getMessage().contains("closed") ||
                                         e.getMessage().contains("endpoint") ||
                                         e.getMessage().contains("disconnected"))) {
                                        logger.debug("Connection closed for {}: {}",
                                            player != null ? player.getPlayerName() : "unknown", e.getMessage());
                                    } else {
                                        logger.warn("Network error for {}: {}",
                                            player != null ? player.getPlayerName() : "unknown", e.getMessage());
                                    }
                                }
                                // Exit run() on read errors
                                return;
                            }
                        }
                        selector.selectedKeys().clear();
                    }

                    if (!outData.isEmpty()) {
                        try {
                            writeOutput();
                        } catch (IOException e) {
                            if (processRunning.get()) {  // Only log if we weren't already shutting down
                                logger.debug("Error writing to socket for {}: {}",
                                    player != null ? player.getPlayerName() : "unknown", e.getMessage());
                            }
                            // Exit run() on write errors
                            return;
                        }
                    }


                    if (player != null) {
                        player.timeOutCounter = 0;
                    }
                } catch (Exception e) {
                    // Only log if this wasn't a normal disconnection
                    if (processRunning.get()) {
                        logger.warn("Error in network loop for {}: {}",
                            player != null ? player.getPlayerName() : "unknown", e.getMessage());
                    }
                    break; // Exit the main loop on any unhandled exception
                }
            }
        } catch (Exception e) {
            logger.error("Fatal error in SocketHandler for {}: {}",
                player != null ? player.getPlayerName() : "unknown", e.getMessage(), e);
        } finally {
            cleanup(); // Ensure cleanup even on exceptions
        }
    }

    private boolean isConnected() {
        return socketChannel != null && socketChannel.isOpen() && socketChannel.isConnected() && !player.disconnected;
    }

    public void logout() {
        if (!processRunning.getAndSet(false)) {
            return; // Already shutting down
        }

        try {
            if (player != null) {
                logger.debug("Initiating logout for player: {}", player.getPlayerName());
                player.disconnected = true;

                // Wake up selector if it's blocked in select()
                if (selector != null && selector.isOpen()) {
                    selector.wakeup();
                }
            }
        } catch (Exception e) {
            logger.warn("Error during logout for player {}: {}",
                    player != null ? player.getPlayerName() : "unknown", e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        if (!processRunning.compareAndSet(true, false)) {
            return;
        }

        outputLock.lock();
        try {
            try {
                // Try to flush any remaining data
                flushRemainingData();
            } catch (Exception e) {
                logger.debug("Error flushing remaining data during cleanup for {}: {}",
                    player.getPlayerName(), e.getMessage());
            }

            if (selector != null) {
                try {
                    selector.close();
                } catch (IOException e) {
                    logger.debug("Error closing selector for {}: {}",
                        player.getPlayerName(), e.getMessage());
                }
            }

            if (socketChannel != null) {
                try {
                    if (socketChannel.isOpen()) {
                        socketChannel.close();
                    }
                } catch (IOException e) {
                    logger.debug("Error closing socket for {}: {}",
                        player.getPlayerName(), e.getMessage());
                }
            }

            logger.info("{} has disconnected.", player.getPlayerName());
        } finally {
            try {
                // Signal any waiting threads that cleanup is complete
                cleanupDone.signalAll();
            } finally {
                try {
                    // Ensure player is marked as disconnected
                    if (player != null) {
                        player.disconnected = true;

                    }
                } finally {
                    outputLock.unlock();
                }
            }
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
            logger.error("Failed to flush remaining data", e);
        } finally {
            outputLock.unlock();
        }
    }

    private void parsePackets() throws IOException {
        try {
            inputBuffer.clear();
            int bytesRead = socketChannel.read(inputBuffer);
            if (bytesRead == -1) {
                throw new IOException("Client disconnected gracefully");
            }

            // Handle connection reset by peer
            if (bytesRead == 0 && !socketChannel.isConnected()) {
                throw new IOException("Connection reset by peer");
            }

            inputBuffer.flip();
            if (inputBuffer.hasRemaining()) {
                packetParser.parsePackets(inputBuffer, player, incomingPackets);
            }
            inputBuffer.compact();
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("reset")) {
                logger.debug("Connection reset by peer for {}: {}", player.getPlayerName(), e.getMessage());
            } else {
                logger.warn("Error reading from socket for {}: {}", player.getPlayerName(), e.getMessage());
            }
            throw e; // Re-throw to trigger cleanup
        }
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
