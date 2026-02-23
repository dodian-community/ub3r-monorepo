package net.dodian.uber.game.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AsyncSqlService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncSqlService.class);

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "AsyncSqlService");
            thread.setDaemon(true);
            return thread;
        }
    });

    private static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean(false);

    private AsyncSqlService() {
    }

    public static void execute(String taskName, Runnable task) {
        if (task == null) {
            return;
        }

        if (SHUTTING_DOWN.get()) {
            logger.warn("Dropping async SQL task '{}' because shutdown is in progress.", taskName);
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                task.run();
            } catch (Exception exception) {
                logger.error("Async SQL task '{}' failed", taskName, exception);
            }
        });
    }

    public static void shutdown(Duration timeout) {
        if (!SHUTTING_DOWN.compareAndSet(false, true)) {
            return;
        }

        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(Math.max(1L, timeout.toMillis()), TimeUnit.MILLISECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            EXECUTOR.shutdownNow();
        }
    }
}
