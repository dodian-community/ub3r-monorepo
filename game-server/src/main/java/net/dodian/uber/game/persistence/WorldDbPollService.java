package net.dodian.uber.game.persistence;

import net.dodian.utilities.DbTables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static net.dodian.utilities.DatabaseKt.getDbConnection;
import static net.dodian.utilities.DotEnvKt.getAsyncWorldDbEnabled;

public final class WorldDbPollService {

    private static final Logger logger = LoggerFactory.getLogger(WorldDbPollService.class);

    private static final ThreadFactory THREAD_FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "WorldDbPollWorker");
        thread.setDaemon(true);
        return thread;
    };

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(THREAD_FACTORY);
    private static final AtomicReference<CompletableFuture<WorldPollResult>> IN_FLIGHT = new AtomicReference<>();
    private static final AtomicReference<WorldPollResult> LATEST_RESULT = new AtomicReference<>(WorldPollResult.EMPTY);
    private static final AtomicBoolean RUNNING = new AtomicBoolean(true);

    private WorldDbPollService() {
    }

    public static CompletableFuture<WorldPollResult> pollAsync(WorldPollInput input) {
        if (input == null) {
            return CompletableFuture.completedFuture(LATEST_RESULT.get());
        }

        if (!getAsyncWorldDbEnabled()) {
            WorldPollResult blocking = runBlockingPoll(input);
            LATEST_RESULT.set(blocking);
            return CompletableFuture.completedFuture(blocking);
        }

        if (!RUNNING.get()) {
            return CompletableFuture.completedFuture(LATEST_RESULT.get());
        }

        CompletableFuture<WorldPollResult> current = IN_FLIGHT.get();
        if (current == null || current.isDone()) {
            CompletableFuture<WorldPollResult> next = CompletableFuture.supplyAsync(() -> runBlockingPoll(input), WORKER);
            if (IN_FLIGHT.compareAndSet(current, next)) {
                next.whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("World DB poll failed", throwable);
                        return;
                    }
                    if (result != null) {
                        LATEST_RESULT.set(result);
                    }
                });
            }
        }

        CompletableFuture<WorldPollResult> inFlight = IN_FLIGHT.get();
        return inFlight == null ? CompletableFuture.completedFuture(LATEST_RESULT.get()) : inFlight;
    }

    public static WorldPollResult getLatestResult() {
        return LATEST_RESULT.get();
    }

    public static WorldPollResult runBlockingPoll(WorldPollInput input) {
        Integer latestNewsId = null;
        Set<Integer> refundReceivers = new HashSet<>();
        Map<Integer, Long> muteTimes = new HashMap<>();
        Set<Integer> bannedPlayers = new HashSet<>();

        try (Connection connection = getDbConnection()) {
            if (input.getWorldId() == 1) {
                updatePlayerCount(connection, input.getWorldId(), input.getPlayerCount());
            }

            latestNewsId = loadLatestNews(connection);

            refundReceivers = loadRefundReceivers(connection);
            if (!refundReceivers.isEmpty()) {
                markRefundMessagesDispatched(connection, refundReceivers);
            }

            loadMuteAndBanState(connection, input.getOnlinePlayerDbIds(), muteTimes, bannedPlayers);
        } catch (Exception exception) {
            logger.error("World DB polling failed", exception);
        }

        return new WorldPollResult(latestNewsId, refundReceivers, muteTimes, bannedPlayers);
    }

    private static void updatePlayerCount(Connection connection, int worldId, int playerCount) throws Exception {
        String query = "UPDATE " + DbTables.GAME_WORLDS + " SET players = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, playerCount);
            statement.setInt(2, worldId);
            statement.executeUpdate();
        }
    }

    private static Integer loadLatestNews(Connection connection) throws Exception {
        String query = "SELECT threadid FROM thread WHERE forumid IN ('98', '99', '101') AND visible = '1' ORDER BY threadid DESC LIMIT 1";
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(query)) {
            if (results.next()) {
                return results.getInt("threadid");
            }
        }
        return null;
    }

    private static Set<Integer> loadRefundReceivers(Connection connection) throws Exception {
        Set<Integer> receivers = new HashSet<>();
        String query = "SELECT DISTINCT receivedBy FROM " + DbTables.GAME_REFUND_ITEMS + " WHERE message='0' AND claimed IS NULL";
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(query)) {
            while (results.next()) {
                receivers.add(results.getInt("receivedBy"));
            }
        }
        return receivers;
    }

    private static void markRefundMessagesDispatched(Connection connection, Set<Integer> receivers) throws Exception {
        String ids = receivers.stream().map(String::valueOf).collect(Collectors.joining(","));
        String updateQuery = "UPDATE " + DbTables.GAME_REFUND_ITEMS +
                " SET message='1' WHERE message='0' AND claimed IS NULL AND receivedBy IN (" + ids + ")";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(updateQuery);
        }
    }

    private static void loadMuteAndBanState(Connection connection,
                                            List<Integer> onlinePlayerDbIds,
                                            Map<Integer, Long> muteTimes,
                                            Set<Integer> bannedPlayers) throws Exception {
        if (onlinePlayerDbIds == null || onlinePlayerDbIds.isEmpty()) {
            return;
        }

        String ids = onlinePlayerDbIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String query = "SELECT id, unmutetime, unbantime FROM " + DbTables.GAME_CHARACTERS + " WHERE id IN (" + ids + ")";
        long now = System.currentTimeMillis();

        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(query)) {
            while (results.next()) {
                int id = results.getInt("id");
                long unmuteTime = results.getLong("unmutetime");
                long unbanTime = results.getLong("unbantime");
                muteTimes.put(id, unmuteTime);

                if (unbanTime > now) {
                    bannedPlayers.add(id);
                }
            }
        }
    }

    public static void shutdown(Duration timeout) {
        RUNNING.set(false);

        CompletableFuture<WorldPollResult> inFlight = IN_FLIGHT.get();
        if (inFlight != null) {
            try {
                inFlight.get(Math.max(1L, timeout.toMillis() / 2), TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
            }
        }

        WORKER.shutdown();
        try {
            if (!WORKER.awaitTermination(Math.max(1L, timeout.toMillis()), TimeUnit.MILLISECONDS)) {
                WORKER.shutdownNow();
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            WORKER.shutdownNow();
        }
    }
}
