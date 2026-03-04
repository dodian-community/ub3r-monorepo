package net.dodian.uber.game.security;

import net.dodian.uber.game.persistence.DbDispatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AsyncSqlService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncSqlService.class);

    private static final ExecutorService EXECUTOR = DbDispatchers.logExecutor;

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
        DbDispatchers.shutdown(EXECUTOR, timeout);
    }
}
