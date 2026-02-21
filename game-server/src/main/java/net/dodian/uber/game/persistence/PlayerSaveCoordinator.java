package net.dodian.uber.game.persistence;

import net.dodian.uber.game.model.entity.player.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static net.dodian.utilities.DotEnvKt.getAsyncPlayerSaveEnabled;
import static net.dodian.utilities.DotEnvKt.getDatabaseSaveBurstAttempts;
import static net.dodian.utilities.DotEnvKt.getDatabaseSaveRetryBaseMs;
import static net.dodian.utilities.DotEnvKt.getDatabaseSaveRetryMaxMs;
import static net.dodian.utilities.DotEnvKt.getDatabaseSaveWorkers;

public class PlayerSaveCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(PlayerSaveCoordinator.class);

    private static final class Holder {
        private static final PlayerSaveCoordinator INSTANCE = new PlayerSaveCoordinator(
                new PlayerSaveRepository(),
                Math.max(1, getDatabaseSaveWorkers()),
                Math.max(50L, getDatabaseSaveRetryBaseMs()),
                Math.max(getDatabaseSaveRetryBaseMs(), getDatabaseSaveRetryMaxMs()),
                Math.max(1, getDatabaseSaveBurstAttempts()),
                true
        );
    }

    private final PlayerSaveRepository repository;
    private final ExecutorService workers;
    private final long retryBaseMs;
    private final long retryMaxMs;
    private final int burstAttempts;

    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean forceStop = new AtomicBoolean(false);
    private final AtomicLong sequence = new AtomicLong(0);

    private final ConcurrentHashMap<Integer, SaveState> states = new ConcurrentHashMap<>();

    private final LongAdder enqueued = new LongAdder();
    private final LongAdder coalesced = new LongAdder();
    private final LongAdder succeeded = new LongAdder();
    private final LongAdder retried = new LongAdder();
    private final LongAdder failed = new LongAdder();

    private final java.util.concurrent.ScheduledExecutorService metricsLogger;

    public PlayerSaveCoordinator(PlayerSaveRepository repository,
                                 int workerCount,
                                 long retryBaseMs,
                                 long retryMaxMs,
                                 int burstAttempts,
                                 boolean startMetricsLogger) {
        this.repository = repository;
        this.retryBaseMs = retryBaseMs;
        this.retryMaxMs = retryMaxMs;
        this.burstAttempts = burstAttempts;

        ThreadFactory workerFactory = runnable -> {
            Thread thread = new Thread(runnable, "PlayerSaveWorker");
            thread.setDaemon(true);
            return thread;
        };

        this.workers = Executors.newFixedThreadPool(Math.max(1, workerCount), workerFactory);
        this.metricsLogger = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "PlayerSaveMetrics");
            thread.setDaemon(true);
            return thread;
        });

        if (startMetricsLogger) {
            this.metricsLogger.scheduleAtFixedRate(this::logMetricsSafe, 30, 30, TimeUnit.SECONDS);
        }
    }

    public static void requestSave(Client client,
                                   PlayerSaveReason reason,
                                   boolean updateProgress,
                                   boolean finalSave) {
        if (client == null || client.dbId < 1) {
            return;
        }

        if (!getAsyncPlayerSaveEnabled()) {
            saveSynchronously(client, reason, updateProgress, finalSave);
            return;
        }

        PlayerSaveCoordinator instance = Holder.INSTANCE;
        PlayerSaveSnapshot snapshot = PlayerSaveSnapshot.fromClient(
                client,
                instance.sequence.incrementAndGet(),
                reason,
                updateProgress,
                finalSave
        );

        instance.requestSave(snapshot);
    }

    private static void saveSynchronously(Client client,
                                          PlayerSaveReason reason,
                                          boolean updateProgress,
                                          boolean finalSave) {
        try {
            PlayerSaveSnapshot snapshot = PlayerSaveSnapshot.fromClient(
                    client,
                    0,
                    reason,
                    updateProgress,
                    finalSave
            );
            new PlayerSaveRepository().saveSnapshot(snapshot);
        } catch (Exception e) {
            logger.error("Synchronous save failed for {} (dbId={})", client.getPlayerName(), client.dbId, e);
        }
    }

    public static boolean isFinalSavePending(int dbId) {
        return Holder.INSTANCE.isFinalSavePendingInternal(dbId);
    }

    public static void shutdownAndDrain(Duration timeout) {
        Holder.INSTANCE.shutdownAndDrainInternal(timeout);
    }

    public boolean hasPendingFinalSave(int dbId) {
        return isFinalSavePendingInternal(dbId);
    }

    public void shutdownAndDrainForTest(Duration timeout) {
        shutdownAndDrainInternal(timeout);
    }

    public void requestSave(PlayerSaveSnapshot snapshot) {
        if (snapshot == null || snapshot.getDbId() < 1) {
            return;
        }

        if (shuttingDown.get() && !snapshot.isFinalSave()) {
            return;
        }

        SaveState state = states.computeIfAbsent(snapshot.getDbId(), ignored -> new SaveState(snapshot.getDbId()));

        synchronized (state) {
            enqueued.increment();

            if (snapshot.isFinalSave()) {
                state.finalSavePending = true;
            }

            if (state.inFlight == null && state.pending == null) {
                state.pending = snapshot;
            } else {
                if (state.pending != null) {
                    coalesced.increment();
                }
                state.pending = snapshot;
            }

            if (!state.running) {
                state.running = true;
                scheduleWorker(state);
            }
        }
    }

    private void scheduleWorker(SaveState state) {
        workers.execute(() -> processStateLoop(state));
    }

    private void processStateLoop(SaveState state) {
        while (!forceStop.get()) {
            PlayerSaveSnapshot current;

            synchronized (state) {
                if (state.inFlight == null) {
                    state.inFlight = state.pending;
                    state.pending = null;
                    state.inFlightAttempt = 0;
                }

                current = state.inFlight;
                if (current == null) {
                    state.running = false;
                    if (!state.finalSavePending) {
                        states.remove(state.dbId, state);
                    }
                    return;
                }
            }

            boolean saved = saveWithRetries(current, state);

            synchronized (state) {
                if (saved) {
                    succeeded.increment();
                    state.inFlight = null;
                    state.inFlightAttempt = 0;

                    if (current.isFinalSave()) {
                        if (state.pending == null || !state.pending.isFinalSave()) {
                            state.finalSavePending = false;
                        }
                    }
                } else {
                    if (!current.isFinalSave()) {
                        failed.increment();
                        state.inFlight = null;
                        state.inFlightAttempt = 0;
                    }
                }

                if (state.inFlight == null && state.pending == null) {
                    if (!state.finalSavePending || forceStop.get()) {
                        state.running = false;
                        states.remove(state.dbId, state);
                        return;
                    }
                }
            }
        }
    }

    private boolean saveWithRetries(PlayerSaveSnapshot snapshot, SaveState state) {
        long backoffMs = retryBaseMs;

        while (!forceStop.get()) {
            try {
                repository.saveSnapshot(snapshot);
                return true;
            } catch (Exception exception) {
                retried.increment();
                state.inFlightAttempt++;

                if (snapshot.isFinalSave()) {
                    if (state.inFlightAttempt <= burstAttempts) {
                        logger.warn(
                                "Final save retry {}/{} for {} (dbId={})",
                                state.inFlightAttempt,
                                burstAttempts,
                                snapshot.getPlayerName(),
                                snapshot.getDbId(),
                                exception
                        );
                        sleepQuietly(backoffMs);
                        backoffMs = Math.min(retryMaxMs, backoffMs * 2);
                        continue;
                    }

                    failed.increment();
                    logger.error(
                            "Final save burst exhausted for {} (dbId={}), entering periodic retry loop.",
                            snapshot.getPlayerName(),
                            snapshot.getDbId(),
                            exception
                    );
                    sleepQuietly(15_000L);
                    continue;
                }

                if (state.inFlightAttempt >= burstAttempts) {
                    logger.error(
                            "Save failed after {} attempts for {} (dbId={})",
                            state.inFlightAttempt,
                            snapshot.getPlayerName(),
                            snapshot.getDbId(),
                            exception
                    );
                    return false;
                }

                sleepQuietly(backoffMs);
                backoffMs = Math.min(retryMaxMs, backoffMs * 2);
            }
        }

        return false;
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isFinalSavePendingInternal(int dbId) {
        SaveState state = states.get(dbId);
        if (state == null) {
            return false;
        }

        synchronized (state) {
            return state.finalSavePending;
        }
    }

    private void shutdownAndDrainInternal(Duration timeout) {
        if (!shuttingDown.compareAndSet(false, true)) {
            return;
        }

        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (isIdle()) {
                break;
            }
            sleepQuietly(50L);
        }

        if (!isIdle()) {
            logger.warn("PlayerSaveCoordinator shutdown timed out with pending saves. Forcing stop.");
            forceStop.set(true);
        }

        workers.shutdown();
        try {
            long remainingNanos = Math.max(0L, deadline - System.nanoTime());
            if (!workers.awaitTermination(Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remainingNanos)), TimeUnit.MILLISECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            workers.shutdownNow();
        }

        metricsLogger.shutdownNow();
    }

    public boolean awaitIdle(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (isIdle()) {
                return true;
            }
            sleepQuietly(25L);
        }
        return isIdle();
    }

    private boolean isIdle() {
        for (SaveState state : states.values()) {
            synchronized (state) {
                if (state.running || state.inFlight != null || state.pending != null || state.finalSavePending) {
                    return false;
                }
            }
        }
        return true;
    }

    private void logMetricsSafe() {
        try {
            long finalLocks = 0;
            long inFlight = 0;
            long pending = 0;

            for (SaveState state : states.values()) {
                synchronized (state) {
                    if (state.finalSavePending) {
                        finalLocks++;
                    }
                    if (state.inFlight != null) {
                        inFlight++;
                    }
                    if (state.pending != null) {
                        pending++;
                    }
                }
            }

            logger.info(
                    "playerSaveMetrics enqueued={} coalesced={} success={} retry={} failure={} inFlight={} pending={} pendingFinalLocks={}",
                    enqueued.sum(),
                    coalesced.sum(),
                    succeeded.sum(),
                    retried.sum(),
                    failed.sum(),
                    inFlight,
                    pending,
                    finalLocks
            );
        } catch (Exception e) {
            logger.warn("Failed to log player save metrics", e);
        }
    }

    private static final class SaveState {
        private final int dbId;
        private volatile boolean running;
        private volatile boolean finalSavePending;

        private PlayerSaveSnapshot inFlight;
        private PlayerSaveSnapshot pending;
        private int inFlightAttempt;

        private SaveState(int dbId) {
            this.dbId = dbId;
        }
    }
}
