package net.dodian.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Single-threaded scheduler driven by the game tick.
 */
public final class GameTickScheduler {

    private static final Logger logger = LoggerFactory.getLogger(GameTickScheduler.class);

    private static final boolean DEBUG_SCHEDULER_LAG = false;

    private final Object lock = new Object();
    private final long tickIntervalMs;
    private final List<ScheduledTask> tasks = new ArrayList<>();

    private ScheduledExecutorService executor;
    private boolean started;
    private long nextPlannedTickTimeMs = -1L;

    public GameTickScheduler(long tickIntervalMs) {
        if (tickIntervalMs <= 0) {
            throw new IllegalArgumentException("tickIntervalMs must be > 0");
        }
        this.tickIntervalMs = tickIntervalMs;
    }

    public void registerTask(String name, long intervalMs, Runnable runnable) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(runnable, "runnable");
        if (intervalMs <= 0) {
            throw new IllegalArgumentException("intervalMs must be > 0");
        }

        synchronized (lock) {
            ScheduledTask task = new ScheduledTask(name, intervalMs, runnable);
            if (started) {
                task.initializeNextRun(System.currentTimeMillis());
            }
            tasks.add(task);
        }
    }

    public void start() {
        synchronized (lock) {
            if (started) {
                return;
            }

            long now = System.currentTimeMillis();
            for (ScheduledTask task : tasks) {
                task.initializeNextRun(now);
            }
            nextPlannedTickTimeMs = now;

            ThreadFactory threadFactory = r -> {
                Thread thread = new Thread(r, "GameTickScheduler");
                thread.setDaemon(true);
                return thread;
            };
            executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
            started = true;
            executor.scheduleAtFixedRate(this::safeTick, 0, tickIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        ScheduledExecutorService toStop;
        synchronized (lock) {
            if (!started && executor == null) {
                return;
            }
            started = false;
            toStop = executor;
            executor = null;
            nextPlannedTickTimeMs = -1L;
            for (ScheduledTask task : tasks) {
                task.reset();
            }
        }

        if (toStop != null) {
            toStop.shutdown();
            try {
                if (!toStop.awaitTermination(5, TimeUnit.SECONDS)) {
                    toStop.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                toStop.shutdownNow();
            }
        }
    }

    private void safeTick() {
        try {
            runTickForTesting(System.currentTimeMillis());
        } catch (Throwable t) {
            logger.error("Game tick scheduler encountered an unexpected error.", t);
        }
    }

    void runTickForTesting(long nowMs) {
        synchronized (lock) {
            if (nextPlannedTickTimeMs == -1L) {
                for (ScheduledTask task : tasks) {
                    task.initializeNextRun(nowMs);
                }
                nextPlannedTickTimeMs = nowMs;
            }

            long plannedTickTime = nextPlannedTickTimeMs;
            nextPlannedTickTimeMs += tickIntervalMs;

            if (DEBUG_SCHEDULER_LAG) {
                long lag = nowMs - plannedTickTime;
                if (lag > 0) {
                    logger.debug("Game tick lag={}ms", lag);
                }
            }

            for (ScheduledTask task : tasks) {
                if (!task.isDue(nowMs)) {
                    continue;
                }
                try {
                    task.runnable.run();
                } catch (Throwable t) {
                    logger.error("Scheduled task '{}' failed.", task.name, t);
                }
                task.advanceNextRun(nowMs);
            }
        }
    }

    private static final class ScheduledTask {
        private final String name;
        private final long intervalMs;
        private final Runnable runnable;
        private long nextRunMs = -1L;

        private ScheduledTask(String name, long intervalMs, Runnable runnable) {
            this.name = name;
            this.intervalMs = intervalMs;
            this.runnable = runnable;
        }

        private void initializeNextRun(long nowMs) {
            if (nextRunMs == -1L) {
                nextRunMs = nowMs;
            }
        }

        private boolean isDue(long nowMs) {
            return nextRunMs != -1L && nowMs >= nextRunMs;
        }

        private void advanceNextRun(long nowMs) {
            do {
                nextRunMs += intervalMs;
            } while (nextRunMs <= nowMs);
        }

        private void reset() {
            nextRunMs = -1L;
        }
    }
}
