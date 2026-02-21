package net.dodian.uber.game.security;

import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.utilities.DbTables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static net.dodian.utilities.DatabaseKt.getDbConnection;
import static net.dodian.utilities.DotEnvKt.getGameWorldId;

public class ChatLog extends LogEntry {

    private static final Logger logger = Logger.getLogger(ChatLog.class.getName());
    private static final BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>();

    private static final Object START_LOCK = new Object();
    private static volatile boolean running = false;
    private static volatile Thread processorThread;

    private static final boolean debugMetrics = true;
    private static final int BATCH_SIZE = 20;

    private static final class ChatMessage {
        final int type;
        final int senderId;
        final int receiverId;
        final String message;

        private ChatMessage(int type, int senderId, int receiverId, String message) {
            this.type = type;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.message = message;
        }
    }

    private static void ensureStarted() {
        if (running) {
            return;
        }
        synchronized (START_LOCK) {
            if (running) {
                return;
            }

            running = true;
            processorThread = new Thread(ChatLog::processMessages, "ChatLog-Processor");
            processorThread.setDaemon(true);
            processorThread.start();
            logger.info("ChatLog processor started.");
        }
    }

    private static void processMessages() {
        while (running || !messageQueue.isEmpty()) {
            try {
                int queueSize = messageQueue.size();

                if (queueSize > 1) {
                    long startTime = System.currentTimeMillis();
                    int processed = processBatch();
                    if (processed > 0 && debugMetrics) {
                        long duration = System.currentTimeMillis() - startTime;
                        System.out.println("[ChatLog Metrics] Batch: Saved " + processed + " messages in " +
                                duration + "ms | Avg: " + (duration / processed) +
                                "ms per message | Queue Size: " + messageQueue.size());
                    }
                } else {
                    ChatMessage message = messageQueue.poll(10, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        long startTime = System.currentTimeMillis();
                        saveMessage(message);
                        if (debugMetrics) {
                            long duration = System.currentTimeMillis() - startTime;
                            System.out.println("[ChatLog Metrics] Direct: Saved message in " + duration + "ms | " +
                                    "Type: " + message.type + " | " +
                                    "Queue Size: " + messageQueue.size());
                        }
                    }
                }
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                logger.warning("ChatLog processor thread interrupted");
            } catch (Exception e) {
                logger.severe("Error processing chat messages: " + e.getMessage());
                e.printStackTrace();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static int processBatch() {
        List<ChatMessage> batch = new ArrayList<>(BATCH_SIZE);
        messageQueue.drainTo(batch, BATCH_SIZE);

        if (batch.isEmpty()) {
            return 0;
        }

        String timestamp = getTimeStamp();
        String query = "INSERT INTO " + DbTables.GAME_CHAT_LOGS +
                "(type, sender, receiver, message, timestamp) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = getDbConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            for (ChatMessage message : batch) {
                statement.setInt(1, message.type);
                statement.setInt(2, message.senderId);
                statement.setInt(3, message.receiverId);
                statement.setString(4, message.message);
                statement.setString(5, timestamp);
                statement.addBatch();
            }

            statement.executeBatch();
            return batch.size();
        } catch (SQLException sqlException) {
            logger.severe("Failed to save chat batch: " + sqlException.getMessage());
            for (ChatMessage message : batch) {
                try {
                    saveMessage(message);
                } catch (Exception fallbackException) {
                    logger.severe("Failed to save individual message: " + fallbackException.getMessage());
                }
            }
            return batch.size();
        }
    }

    private static void saveMessage(ChatMessage message) {
        String query = "INSERT INTO " + DbTables.GAME_CHAT_LOGS +
                "(type, sender, receiver, message, timestamp) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = getDbConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, message.type);
            statement.setInt(2, message.senderId);
            statement.setInt(3, message.receiverId);
            statement.setString(4, message.message);
            statement.setString(5, getTimeStamp());
            statement.executeUpdate();
        } catch (SQLException sqlException) {
            logger.severe("Unable to record chat message! " + sqlException.getMessage());
            sqlException.printStackTrace();
            if (message.type == 1 || message.type == 2) {
                YellSystem.alertStaff("Unable to record chat, please contact an admin.");
            }
        }
    }

    public static void recordPublicChat(Player player, String message) {
        if (getGameWorldId() > 1) {
            return;
        }
        ensureStarted();
        messageQueue.add(new ChatMessage(1, player.dbId, -1, sanitizeMessage(message)));
    }

    public static void recordYellChat(Player player, String message) {
        if (getGameWorldId() > 1) {
            return;
        }
        ensureStarted();
        messageQueue.add(new ChatMessage(2, player.dbId, -1, sanitizeMessage(message)));
    }

    public static void recordModChat(Player player, String message) {
        if (getGameWorldId() > 1) {
            return;
        }
        ensureStarted();
        messageQueue.add(new ChatMessage(4, player.dbId, -1, sanitizeMessage(message)));
    }

    public static void recordPrivateChat(Player sender, Player receiver, String message) {
        if (getGameWorldId() > 1) {
            return;
        }
        ensureStarted();
        messageQueue.add(new ChatMessage(3, sender.dbId, receiver.dbId, sanitizeMessage(message)));
    }

    private static String sanitizeMessage(String message) {
        return message.replaceAll("'", "`").replaceAll("\\\\", "/");
    }

    public static void shutdown() {
        synchronized (START_LOCK) {
            running = false;
            if (processorThread != null) {
                processorThread.interrupt();
                try {
                    processorThread.join(5_000L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
                processorThread = null;
            }
        }
    }

    public static void main(String[] args) {
        try (Connection connection = getDbConnection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT * FROM " + DbTables.GAME_CHAT_LOGS + " ORDER BY timestamp DESC LIMIT 50")) {

            System.out.println("=== Last 50 Chat Logs ===");
            System.out.printf("%-5s %-8s %-20s %-30s %-20s%n",
                    "Type", "Sender", "Receiver", "Message", "Timestamp");

            while (results.next()) {
                int type = results.getInt("type");
                int sender = results.getInt("sender");
                int receiver = results.getInt("receiver");
                String message = results.getString("message");
                String timestamp = results.getTimestamp("timestamp").toString();

                String typeStr;
                switch (type) {
                    case 1:
                        typeStr = "Public";
                        break;
                    case 2:
                        typeStr = "Yell";
                        break;
                    case 3:
                        typeStr = "Private";
                        break;
                    case 4:
                        typeStr = "Mod";
                        break;
                    default:
                        typeStr = "Unknown";
                }

                System.out.printf("%-5s %-8d %-20d %-20.20s %-30.30s %-20s%n",
                        typeStr, sender, receiver, message, "...", timestamp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
