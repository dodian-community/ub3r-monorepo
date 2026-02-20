package net.dodian.jobs.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.utilities.DbTables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static net.dodian.utilities.DotEnvKt.getDatabaseHost;
import static net.dodian.utilities.DotEnvKt.getDatabasePort;
import static net.dodian.utilities.DotEnvKt.getDatabaseName;
import static net.dodian.utilities.DotEnvKt.getDatabaseUsername;
import static net.dodian.utilities.DotEnvKt.getDatabasePassword;
import static net.dodian.utilities.DotEnvKt.getGameWorldId;

public class WorldProcessor implements Runnable {

    private static final HikariDataSource dataSource;
    private static Integer cachedLatestNewsId = null;
    private static final Set<Integer> playersWithRefunds = ConcurrentHashMap.newKeySet();
    private static final Map<Integer, Long> playerMuteTimes = new ConcurrentHashMap<>();


    static {
        try {
            HikariConfig config = new HikariConfig();
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?serverTimezone=UTC",
                    getDatabaseHost(),
                    getDatabasePort(),
                    getDatabaseName());
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(getDatabaseUsername());
            config.setPassword(getDatabasePassword());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            config.setMinimumIdle(2);
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            System.out.println("WorldProcessor: HikariCP connection pool initialized.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (dataSource != null && !dataSource.isClosed()) {
                    System.out.println("WorldProcessor: Closing HikariCP connection pool...");
                    dataSource.close();
                }
            }));
        } catch (Exception e) {
            System.err.println("FATAL: WorldProcessor could not initialize HikariCP connection pool!");
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database connection pool for WorldProcessor", e);
        }
    }

    private Connection getPooledConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("WorldProcessor: HikariDataSource is not initialized!");
        }
        return dataSource.getConnection();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("WorldProcessor-Thread");
        try {
            if (getGameWorldId() == 1) {
                updatePlayerCount();
            }
            checkForumUpdatesAndNotify();
            processRefunds();
            processMutesAndBans();
            Server.chat.clear();
        } catch (Exception e) {
            System.err.println("Critical error in WorldProcessor run: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Thread.currentThread().setName("WorldProcessor-Thread-Idle");
        }
    }

    private void updatePlayerCount() {
        if (getGameWorldId() != 1) return;
        String query = "UPDATE " + DbTables.GAME_WORLDS + " SET players = ? WHERE id = ?";
        try (Connection conn = getPooledConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, PlayerHandler.getPlayerCount());
            pstmt.setInt(2, getGameWorldId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to update player count: " + e.getMessage());
        }
    }

    private void checkForumUpdatesAndNotify() {
        String query = "SELECT threadid FROM thread WHERE forumid IN ('98', '99', '101') AND visible = '1' ORDER BY threadid DESC LIMIT 1";
        try (Connection conn = getPooledConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                int latestNews = rs.getInt("threadid");
                if (cachedLatestNewsId == null || latestNews > cachedLatestNewsId) {
                    cachedLatestNewsId = latestNews; // Modifies shared static state
                    notifyPlayersOfNews(latestNews); // Modifies Client objects (c.latestNews) and sends a packet
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking forum updates: " + e.getMessage());
        }
    }

    private void notifyPlayersOfNews(int newsId) {
        Arrays.stream(PlayerHandler.players)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof Client)
                .map(p -> (Client) p)
                .filter(c -> c.loadingDone && c.latestNews != newsId)
                .forEach(c -> {
                    c.latestNews = newsId; // Direct modification of Client state
                    c.send(new SendMessage("[SERVER]: There is a new post on the homepage! type ::news"));
                });
    }

    private void processRefunds() {
        playersWithRefunds.clear();
        String selectQuery = "SELECT DISTINCT receivedBy FROM " + DbTables.GAME_REFUND_ITEMS +
                " WHERE message='0' AND claimed IS NULL";
        try (Connection conn = getPooledConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectQuery)) {
            while (rs.next()) {
                playersWithRefunds.add(rs.getInt("receivedBy"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching players with refunds: " + e.getMessage());
            return;
        }

        if (!playersWithRefunds.isEmpty()) {
            Arrays.stream(PlayerHandler.players)
                    .filter(Objects::nonNull)
                    .filter(p -> p instanceof Client)
                    .map(p -> (Client) p)
                    .filter(c -> c.loadingDone && playersWithRefunds.contains(c.dbId))
                    .forEach(c -> c.send(new SendMessage("<col=4C4B73>You have some unclaimed items to claim!"))); // Sends packet

            String updateQuery = "UPDATE " + DbTables.GAME_REFUND_ITEMS +
                    " SET message='1' WHERE message='0' AND claimed IS NULL AND receivedBy IN (" +
                    playersWithRefunds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
            try (Connection conn = getPooledConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(updateQuery);
            } catch (SQLException e) {
                System.err.println("Error updating refund item messages: " + e.getMessage());
            }
        }
    }

    private void processMutesAndBans() {

        List<Integer> onlinePlayerDbIds = Arrays.stream(PlayerHandler.players)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof Client)
                .map(p -> ((Client) p).dbId)
                .collect(Collectors.toList());

        if (onlinePlayerDbIds.isEmpty()) {
            return;
        }

        String idsForQuery = onlinePlayerDbIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String muteQuery = "SELECT id, unmutetime FROM " + DbTables.GAME_CHARACTERS + " WHERE id IN (" + idsForQuery + ")";

        Map<Integer, Long> currentMutesFromDb = new HashMap<>();
        try (Connection conn = getPooledConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(muteQuery)) {
            while (rs.next()) {
                currentMutesFromDb.put(rs.getInt("id"), rs.getLong("unmutetime"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching mute times: " + e.getMessage());
        }

        playerMuteTimes.clear();
        playerMuteTimes.putAll(currentMutesFromDb);

        Arrays.stream(PlayerHandler.players)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof Client)
                .map(p -> (Client) p)
                .forEach(c -> {
                    Long muteTime = playerMuteTimes.get(c.dbId);
                    if (muteTime != null && c.mutedTill != muteTime) {
                        c.mutedTill = muteTime;
                    }

                    try {

                        if (Server.loginManager.isBanned(c.dbId)) {
                            c.disconnected = true;
                        }
                    } catch (SQLException eBan) {
                        System.err.println("SQLException checking ban status for player " + c.dbId + ": " + eBan.getMessage());

                    } catch (Exception eBanGeneric) {
                        System.err.println("Unexpected error checking ban status for player " + c.dbId + ": " + eBanGeneric.getMessage());
                    }
                });
    }
}
