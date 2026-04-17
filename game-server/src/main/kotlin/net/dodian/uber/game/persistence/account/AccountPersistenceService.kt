package net.dodian.uber.game.persistence.account

import java.time.Duration
import java.util.function.Consumer
import java.util.function.IntConsumer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.player.PlayerSaveReason
import net.dodian.uber.game.persistence.DbDispatchers
import net.dodian.uber.game.persistence.account.login.AccountLoginService
import net.dodian.uber.game.persistence.player.PlayerSaveService
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import net.dodian.uber.game.persistence.repository.DbResult
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.engine.loop.GameThreadTaskQueue
import net.dodian.uber.game.engine.tasking.PlayerScopedCoroutineService
import org.slf4j.LoggerFactory
import net.dodian.uber.game.persistence.db.DbTables

object AccountPersistenceService {
    private val logger = LoggerFactory.getLogger(AccountPersistenceService::class.java)
    @JvmField
    val dispatcher = DbDispatchers.accountDispatcher

    @JvmField
    val scope = CoroutineScope(SupervisorJob() + dispatcher)

    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
    @JvmStatic
    fun submitLoginLoad(
        client: Client,
        username: String,
        password: String,
        onComplete: Consumer<LoginLoadResult>,
    ) {
        scope.launch {
            val startedAt = System.nanoTime()
            var pendingRetries = 0
            val code =
                try {
                    val deadline = System.currentTimeMillis() + 3_000L
                    var finalCode = 13
                    while (true) {
                        val loadCode = AccountLoginService.loadGame(client, username, password)
                        if (loadCode != AccountLoginService.FINAL_SAVE_PENDING_INTERNAL) {
                            finalCode = loadCode
                            break
                        }
                        pendingRetries++
                        if (System.currentTimeMillis() >= deadline) {
                            finalCode = 5
                            break
                        }
                        delay(50L)
                    }
                    finalCode
                } catch (exception: CancellationException) {
                    logger.info("Account load cancelled for {}", username)
                    13
                } catch (exception: RuntimeException) {
                    logger.warn("Account load failed for {}", username, exception)
                    13
                }
            val durationMs = (System.nanoTime() - startedAt) / 1_000_000L
            onComplete.accept(LoginLoadResult(code, durationMs, pendingRetries))
        }
    }

    @JvmStatic
    fun submitLoginLoadCodeOnly(
        client: Client,
        username: String,
        password: String,
        onComplete: IntConsumer,
    ) {
        submitLoginLoad(client, username, password) { result -> onComplete.accept(result.code) }
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
    fun submitRefundCheck(client: Client) {
        val dbId = client.dbId
        if (dbId < 1) {
            return
        }
        PlayerScopedCoroutineService.launch(
            player = client,
            jobKey = "refund-check",
            scope = scope,
        ) {
            val hasUnclaimedResult =
                DbAsyncRepository.suspendReadConnection(dispatcher) { conn ->
                    conn.prepareStatement(
                        "SELECT 1 FROM ${DbTables.GAME_REFUND_ITEMS} " +
                            "WHERE receivedBy=? AND message='0' AND claimed IS NULL LIMIT 1",
                    ).use { ps ->
                        ps.setInt(1, dbId)
                        ps.executeQuery().use { rs -> rs.next() }
                    }
                }
            val hasUnclaimed =
                when (hasUnclaimedResult) {
                    is DbResult.Success -> hasUnclaimedResult.value
                    is DbResult.Failure -> {
                        logger.debug("Refund check failed for dbId={}", dbId, hasUnclaimedResult.error)
                        false
                    }
                }

            if (!hasUnclaimed) {
                return@launch
            }

            val updateResult =
                DbAsyncRepository.suspendReadConnection(dispatcher) { conn ->
                    conn.prepareStatement(
                        "UPDATE ${DbTables.GAME_REFUND_ITEMS} SET message='1' " +
                            "WHERE receivedBy=? AND message='0' AND claimed IS NULL",
                    ).use { ps ->
                        ps.setInt(1, dbId)
                        ps.executeUpdate()
                    }
                    true
                }
            if (updateResult is DbResult.Failure) {
                logger.debug("Refund message update failed for dbId={}", dbId, updateResult.error)
            }

            GameThreadTaskQueue.submit {
                if (client.disconnected) {
                    return@submit
                }
                client.sendMessage("<col=4C4B73>You have some unclaimed items to claim!")
            }
        }
    }

    @JvmStatic
    fun shutdownAndDrain(timeout: Duration) {
        PlayerSaveService.shutdownAndDrain(timeout)
        DbDispatchers.shutdown(DbDispatchers.accountExecutor, timeout)
    }

    data class LoginLoadResult(
        val code: Int,
        val durationMs: Long,
        val pendingRetries: Int,
    )
}
