package net.dodian.uber.game.security;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.dodian.uber.game.model.YellSystem;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.utilities.DbTables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;
import static net.dodian.utilities.DotEnvKt.getDatabaseHost;
import static net.dodian.utilities.DotEnvKt.getDatabasePort;
import static net.dodian.utilities.DotEnvKt.getDatabaseName;
import static net.dodian.utilities.DotEnvKt.getDatabaseUsername;
import static net.dodian.utilities.DotEnvKt.getDatabasePassword;

public class ChatLog extends LogEntry {

    private static final Logger logger = Logger.getLogger(ChatLog.class.getName());
    private static final HikariDataSource dataSource;
    private static final BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>();
    private static volatile boolean running = true;
    private static final boolean debugMetrics = true;
    

    private static final int BATCH_SIZE = 20; // Process this many messages at once
    private static final long MAX_BATCH_WAIT_MS = 10; //this is really fast porbably can be changed to 500
    
    static {
        // Initialize connection pool
        HikariConfig config = new HikariConfig();
        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?serverTimezone=UTC&rewriteBatchedStatements=true",
                getDatabaseHost(),
                getDatabasePort(),
                getDatabaseName());
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(getDatabaseUsername());
        config.setPassword(getDatabasePassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMinimumIdle(3);
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
        logger.info("ChatLog: HikariCP connection pool initialized.");

        // Start processing thread
        Thread processorThread = new Thread(ChatLog::processMessages, "ChatLog-Processor");
        processorThread.setDaemon(true);
        processorThread.start();

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
        }));
    }

    private static Connection getPooledConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private static void processMessages() {
        while (running) {
            try {
                // Check queue size to determine strategy
                int queueSize = messageQueue.size();
                
                if (queueSize > 1) {
                    // Multiple messages available - use batch processing
                    long startTime = System.currentTimeMillis();
                    int processed = processBatch();
                    if (processed > 0 && debugMetrics) {
                        long duration = System.currentTimeMillis() - startTime;
                        System.out.println("[ChatLog Metrics] Batch: Saved " + processed + " messages in " + 
                                          duration + "ms | Avg: " + (duration / processed) + 
                                          "ms per message | Queue Size: " + messageQueue.size());
                    }
                } else {
                    // Single or no message - process directly without waiting
                    ChatMessage message = messageQueue.poll();
                    if (message != null) {
                        long startTime = System.currentTimeMillis();
                        saveMessage(message);
                        if (debugMetrics) {
                            long duration = System.currentTimeMillis() - startTime;
                            System.out.println("[ChatLog Metrics] Direct: Saved message in " + duration + "ms | " +
                                    "Type: " + message.type + " | " +
                                    "Queue Size: " + messageQueue.size());
                        }
                    } else {
                        // No messages available, short sleep
                        Thread.sleep(10);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warning("ChatLog processor thread interrupted");
            } catch (Exception e) {
                logger.severe("Error processing chat messages: " + e.getMessage());
                e.printStackTrace();
                try {
                    Thread.sleep(100); // Shorter wait on error
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static int processBatch() throws SQLException {
        List<ChatMessage> batch = new ArrayList<>(BATCH_SIZE);
        
        // Get available messages up to batch size without waiting
        messageQueue.drainTo(batch, BATCH_SIZE);
        
        if (batch.isEmpty()) {
            return 0;
        }
        
        String timestamp = getTimeStamp();
        String query = "INSERT INTO " + DbTables.GAME_CHAT_LOGS + 
                      "(type, sender, receiver, message, timestamp) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = getPooledConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            for (ChatMessage msg : batch) {
                pstmt.setInt(1, msg.type);
                pstmt.setInt(2, msg.senderId);
                pstmt.setInt(3, msg.receiverId);
                pstmt.setString(4, msg.message);
                pstmt.setString(5, timestamp);
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            return batch.size();
        } catch (SQLException e) {
            logger.severe("Failed to save chat batch: " + e.getMessage());
            // Fall back to individual processing on batch failure
            for (ChatMessage msg : batch) {
                try {
                    saveMessage(msg);
                } catch (Exception ex) {
                    logger.severe("Failed to save individual message: " + ex.getMessage());
                }
            }
            return batch.size();
        }
    }

    private static void saveMessage(ChatMessage message) {
        String query = "INSERT INTO " + DbTables.GAME_CHAT_LOGS + 
                      "(type, sender, receiver, message, timestamp) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = getPooledConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, message.type);
            pstmt.setInt(2, message.senderId);
            pstmt.setInt(3, message.receiverId);
            pstmt.setString(4, message.message);
            pstmt.setString(5, getTimeStamp());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Unable to record chat message! " + e.getMessage());
            e.printStackTrace();
            if (message.type == 1 || message.type == 2) {
                YellSystem.alertStaff("Unable to record chat, please contact an admin.");
            }
        }
    }

    private static class ChatMessage {
        final int type;
        final int senderId;
        final int receiverId;
        final String message;

        ChatMessage(int type, int senderId, int receiverId, String message) {
            this.type = type;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.message = message;
        }
    }

    public static void recordPublicChat(Player player, String message) {
        if (getGameWorldId() > 1) return;
        message = sanitizeMessage(message);
        messageQueue.add(new ChatMessage(1, player.dbId, -1, message));
    }

    public static void recordYellChat(Player player, String message) {
        if (getGameWorldId() > 1) return;
        message = sanitizeMessage(message);
        messageQueue.add(new ChatMessage(2, player.dbId, -1, message));
    }

    public static void recordModChat(Player player, String message) {
        if (getGameWorldId() > 1) return;
        message = sanitizeMessage(message);
        messageQueue.add(new ChatMessage(4, player.dbId, -1, message));
    }

    public static void recordPrivateChat(Player sender, Player receiver, String message) {
        if (getGameWorldId() > 1) return;
        message = sanitizeMessage(message);
        messageQueue.add(new ChatMessage(3, sender.dbId, receiver.dbId, message));
    }

    private static String sanitizeMessage(String message) {
        return message.replaceAll("'", "`").replaceAll("\\\\", "/");
    }

    // Temporary main method for verification
    public static void main(String[] args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + DbTables.GAME_CHAT_LOGS + " ORDER BY timestamp DESC LIMIT 50")) {
            
            System.out.println("=== Last 50 Chat Logs ===");
            System.out.printf("%-5s %-8s %-20s %-30s %-20s%n", 
                "Type", "Sender", "Receiver", "Message", "Timestamp");
            
            while (rs.next()) {
                int type = rs.getInt("type");
                int sender = rs.getInt("sender");
                int receiver = rs.getInt("receiver");
                String message = rs.getString("message");
                String timestamp = rs.getTimestamp("timestamp").toString();
                
                String typeStr;
                switch(type) {
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
