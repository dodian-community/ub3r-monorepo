package net.dodian.uber.game.persistence.account

import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.function.IntConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.PlayerSaveReason
import net.dodian.uber.game.persistence.v2.PlayerSaveService
import org.slf4j.LoggerFactory

object AccountPersistenceService {
    private val logger = LoggerFactory.getLogger(AccountPersistenceService::class.java)
    private val executor =
        Executors.newSingleThreadExecutor(ThreadFactory { runnable ->
            Thread(runnable, "account-db").apply { isDaemon = true }
        })

    @JvmField
    val dispatcher = executor.asCoroutineDispatcher()

    @JvmField
    val scope = CoroutineScope(SupervisorJob() + dispatcher)

    @JvmStatic
    fun submitLoginLoad(
        client: Client,
        username: String,
        password: String,
        onComplete: IntConsumer,
    ) {
        scope.launch {
            val result =
                try {
                    Server.loginManager.loadgame(client, username, password)
                } catch (exception: Exception) {
                    logger.warn("Account load failed for {}", username, exception)
                    13
                }
            onComplete.accept(result)
        }
    }

    @JvmStatic
    fun requestSave(
        client: Client,
        reason: PlayerSaveReason,
        updateProgress: Boolean,
        finalSave: Boolean,
    ) {
        PlayerSaveService.requestSave(client, reason, updateProgress, finalSave)
    }

    @JvmStatic
    fun saveSynchronously(
        client: Client,
        reason: PlayerSaveReason,
        updateProgress: Boolean,
        finalSave: Boolean,
    ) {
        PlayerSaveService.saveSynchronously(client, reason, updateProgress, finalSave)
    }

    @JvmStatic
    fun isFinalSavePending(dbId: Int): Boolean = PlayerSaveService.isFinalSavePending(dbId)

    @JvmStatic
    fun shutdownAndDrain(timeout: Duration) {
        PlayerSaveService.shutdownAndDrain(timeout)
        executor.shutdown()
        val waitMs = timeout.toMillis().coerceAtLeast(1L)
        if (!executor.awaitTermination(waitMs, TimeUnit.MILLISECONDS)) {
            executor.shutdownNow()
        }
    }
}
