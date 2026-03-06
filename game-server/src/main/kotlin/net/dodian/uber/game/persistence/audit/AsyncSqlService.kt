package net.dodian.uber.game.persistence.audit

import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import net.dodian.uber.game.persistence.DbDispatchers
import org.slf4j.LoggerFactory

object AsyncSqlService {
    private val logger = LoggerFactory.getLogger(AsyncSqlService::class.java)
    private val shuttingDown = AtomicBoolean(false)

    @JvmStatic
    fun execute(taskName: String, task: Runnable?) {
        if (task == null) return
        if (shuttingDown.get()) {
            logger.warn("Dropping async SQL task '{}' because shutdown is in progress.", taskName)
            return
        }
        DbDispatchers.logExecutor.execute {
            try {
                task.run()
            } catch (exception: Exception) {
                logger.error("Async SQL task '{}' failed", taskName, exception)
            }
        }
    }

    @JvmStatic
    fun shutdown(timeout: Duration) {
        if (!shuttingDown.compareAndSet(false, true)) return
        DbDispatchers.shutdown(DbDispatchers.logExecutor, timeout)
    }
}
