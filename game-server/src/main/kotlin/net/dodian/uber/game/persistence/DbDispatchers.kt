package net.dodian.uber.game.persistence

import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

object DbDispatchers {
    private fun newSingleThreadExecutor(threadName: String): ExecutorService =
        Executors.newSingleThreadExecutor(
            ThreadFactory { runnable ->
                Thread(runnable, threadName).apply { isDaemon = true }
            },
        )

    @JvmField
    val accountExecutor: ExecutorService = newSingleThreadExecutor("account-db")

    @JvmField
    val worldExecutor: ExecutorService = newSingleThreadExecutor("world-db")

    @JvmField
    val logExecutor: ExecutorService = newSingleThreadExecutor("log-db")

    @JvmField
    val accountDispatcher: CoroutineDispatcher = accountExecutor.asCoroutineDispatcher()

    @JvmField
    val worldDispatcher: CoroutineDispatcher = worldExecutor.asCoroutineDispatcher()

    @JvmField
    val logDispatcher: CoroutineDispatcher = logExecutor.asCoroutineDispatcher()

    @JvmStatic
    fun shutdown(executor: ExecutorService, timeout: Duration) {
        executor.shutdown()
        val waitMs = timeout.toMillis().coerceAtLeast(1L)
        if (!executor.awaitTermination(waitMs, TimeUnit.MILLISECONDS)) {
            executor.shutdownNow()
        }
    }
}

