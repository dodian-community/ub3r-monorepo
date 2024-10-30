package net.dodian.uber.game;

import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.utilities.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class ServerConnectionHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(ServerConnectionHandler.class.getName());
    private static final int DELAY = 50;
    private static final int MAX_CONNECTIONS_PER_SECOND = 3;
    private static final int MAX_VIOLATIONS = 3;

    // Ban duration constants (in milliseconds)
    private static final long MINUTE = 60 * 1000;
    private static final long FIRST_BAN_DURATION = 5 * MINUTE;  // 5 minutes
    private static final long SECOND_BAN_DURATION = 10 * MINUTE; // 10 minutes

    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private boolean shutdownHandler = false;
    private final PlayerHandler playerHandler;
    private final ExecutorService connectionExecutor;

    // Connection tracking and security fields
    private final Map<String, ConnectionTracker> connectionTrackers = new ConcurrentHashMap<>();
    private final Set<String> blacklistedIPs = ConcurrentHashMap.newKeySet();
    private final Map<String, BlacklistEntry> blacklistEntries = new ConcurrentHashMap<>();

    // Inner class to track blacklist entries
    private static class BlacklistEntry {
        private final long banStartTime;
        private final long banDuration; // -1 for permanent (until restart)
        private final int violationCount;

        public BlacklistEntry(long banDuration, int violationCount) {
            this.banStartTime = System.currentTimeMillis();
            this.banDuration = banDuration;
            this.violationCount = violationCount;
        }

        public boolean isPermanent() {
            return banDuration == -1;
        }

        public boolean hasExpired() {
            return !isPermanent() && System.currentTimeMillis() > (banStartTime + banDuration);
        }

        public long getRemainingTime() {
            if (isPermanent()) {
                return -1;
            }
            return Math.max(0, (banStartTime + banDuration) - System.currentTimeMillis());
        }
    }

    // Inner class to track connections
    private static class ConnectionTracker {
        private final Queue<Long> connectionTimestamps = new ConcurrentLinkedQueue<>();
        private int violationCount = 0;

        public boolean checkAndAddConnection() {
            long currentTime = System.currentTimeMillis();

            // Remove timestamps older than 1 second
            while (!connectionTimestamps.isEmpty() &&
                    connectionTimestamps.peek() < currentTime - 1000) {
                connectionTimestamps.poll();
            }

            // Check if we're under the limit
            if (connectionTimestamps.size() < MAX_CONNECTIONS_PER_SECOND) {
                connectionTimestamps.offer(currentTime);
                return true;
            }

            // Connection limit exceeded
            violationCount++;
            return false;
        }

        public int getViolationCount() {
            return violationCount;
        }
    }

    public ServerConnectionHandler(int port, PlayerHandler playerHandler) throws IOException {
        this.playerHandler = playerHandler;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress(port));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.connectionExecutor = Executors.newCachedThreadPool();
        logger.info("ServerConnectionHandler initialized on port " + port);
    }

    @Override
    public void run() {
        try {
            while (!shutdownHandler) {
                selector.select(DELAY);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        acceptConnection();
                    }
                }
            }
        } catch (IOException e) {
            if (!shutdownHandler) {
                logger.severe("Server error: " + e.getMessage());
            } else {
                logger.info("ServerConnectionHandler was shut down.");
            }
        } finally {
            shutdown();
        }
    }

    private void acceptConnection() {
        try {
            final SocketChannel socketChannel = serverSocketChannel.accept();
            if (socketChannel != null) {
                socketChannel.configureBlocking(false);
                final String connectingHost = extractConnectingHost(socketChannel);

                if (Server.antiddos && !Server.tempConns.containsKey(connectingHost)) {
                    socketChannel.close();
                } else {
                    Server.tempConns.remove(connectingHost);
                    Server.connections.add(connectingHost);
                    if (checkHost(connectingHost)) {
                        connectionExecutor.submit(() -> {
                            try {
                                playerHandler.newPlayerClient(socketChannel, connectingHost);
                            } catch (Exception e) {
                                logger.severe("Error processing new client connection: " + e.getMessage());
                                closeSocketChannel(socketChannel);
                            }
                        });
                    } else {
                        socketChannel.close();
                    }
                }
            }
        } catch (IOException e) {
            logger.warning("Error accepting connection: " + e.getMessage());
        }
    }

    private String extractConnectingHost(SocketChannel socketChannel) throws IOException {
        String fullAddress = socketChannel.getRemoteAddress().toString();
        return fullAddress.substring(1, fullAddress.indexOf(":"));
    }

    private void closeSocketChannel(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (IOException e) {
            logger.warning("Error closing socket channel: " + e.getMessage());
        }
    }

    private String formatDuration(long duration) {
        if (duration == -1) {
            return "until server restart";
        }
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        return minutes + " minutes";
    }

    private boolean checkHost(String host) {
        // Clean up expired blacklist entries
        blacklistEntries.entrySet().removeIf(entry -> {
            if (!entry.getValue().isPermanent() && entry.getValue().hasExpired()) {
                blacklistedIPs.remove(entry.getKey());
                return true;
            }
            return false;
        });

        // Check if IP is blacklisted
        BlacklistEntry entry = blacklistEntries.get(host);
        if (entry != null) {
            if (entry.isPermanent() || !entry.hasExpired()) {
                String banStatus = entry.isPermanent() ?
                        "permanently banned (until server restart)" :
                        "banned for " + formatDuration(entry.getRemainingTime());
                logger.warning("Rejected connection from blacklisted IP: " + host +
                        " (" + banStatus + ")");
                return false;
            }
        }

        // Get or create connection tracker for this IP
        ConnectionTracker tracker = connectionTrackers.computeIfAbsent(host,
                k -> new ConnectionTracker());

        // Check connection rate
        if (!tracker.checkAndAddConnection()) {
            logger.warning("Connection rate limit exceeded for IP: " + host);

            // Determine ban action based on violation count
            if (tracker.getViolationCount() >= MAX_VIOLATIONS) {
                blacklistIPs(host, tracker.getViolationCount());
            } else {
                applyTemporaryBan(host, tracker.getViolationCount());
            }
            return false;
        }

        return true;
    }

    private void applyTemporaryBan(String ip, int violationCount) {
        long banDuration;
        switch (violationCount) {
            case 1:
                banDuration = FIRST_BAN_DURATION;
                break;
            case 2:
                banDuration = SECOND_BAN_DURATION;
                break;
            default:
                blacklistIPs(ip, violationCount);
                return;
        }

        BlacklistEntry entry = new BlacklistEntry(banDuration, violationCount);
        blacklistEntries.put(ip, entry);
        blacklistedIPs.add(ip);

        logger.warning("IP address " + ip + " temporarily banned for " +
                formatDuration(banDuration) + " (Violation #" + violationCount + ")");
    }

    private void blacklistIPs(String ip, int violationCount) {
        if (violationCount >= MAX_VIOLATIONS) {
            BlacklistEntry entry = new BlacklistEntry(-1, violationCount); // Permanent ban
            blacklistEntries.put(ip, entry);
            blacklistedIPs.add(ip);
            logger.warning("IP address " + ip + " permanently banned until server restart " +
                    "(Maximum violations reached)");
        }
    }

    public void removeFromBlacklist(String ip) {
        blacklistedIPs.remove(ip);
        blacklistEntries.remove(ip);
        connectionTrackers.remove(ip);
        logger.info("IP address removed from blacklist: " + ip);
    }

    public Map<String, String> getBlacklistStatus() {
        Map<String, String> status = new HashMap<>();
        blacklistEntries.forEach((ip, entry) -> {
            if (entry.isPermanent() || !entry.hasExpired()) {
                String banStatus = entry.isPermanent() ?
                        "Permanently banned until server restart" :
                        "Banned for " + formatDuration(entry.getRemainingTime());
                status.put(ip, "Violation #" + entry.violationCount + " - " + banStatus);
            }
        });
        return status;
    }

    public void shutdown() {
        shutdownHandler = true;
        connectionExecutor.shutdown();
        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
            if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
                serverSocketChannel.close();
            }
        } catch (IOException e) {
            logger.severe("Error during server shutdown: " + e.getMessage());
        }
    }
}
