package net.dodian.uber.game.persistence.account

import java.time.Duration
import java.util.function.Consumer
import java.util.function.IntConsumer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.player.PlayerSaveReason
import net.dodian.uber.game.persistence.DbDispatchers
import net.dodian.uber.game.persistence.account.login.AccountLoginService
import net.dodian.uber.game.persistence.player.PlayerSaveService
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import net.dodian.uber.game.persistence.repository.DbResult
import net.dodian.uber.game.engine.loop.GameThreadTaskQueue
import net.dodian.uber.game.engine.tasking.PlayerScopedCoroutineService
import net.dodian.uber.game.engine.loop.TickThreadBlockingGuard
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
    @JvmOverloads
    fun submitLoginLoad(
        client: Client,
        username: String,
        password: String,
        discordAuthCode: String = "",
        accountCreationRequested: Boolean = discordAuthCode.isNotBlank(),
        onComplete: Consumer<LoginLoadResult>,
    ) {
        scope.launch {
            val startedAt = System.nanoTime()
            var pendingRetries = 0
            val prepared =
                try {
                    // Check before exchanging the one-time Discord OAuth code. This lets the
                    // client keep that authorization and submit a different username instead of
                    // forcing the player through Discord again for a simple name collision.
                    val creationPreflightCode = accountCreationPreflightCode(
                        accountCreationRequested = accountCreationRequested,
                        discordAuthCode = discordAuthCode,
                        usernameExists = { AccountLoginService.usernameExists(username) },
                    )
                    if (creationPreflightCode != null) {
                        if (creationPreflightCode == AccountLoginService.USERNAME_TAKEN) {
                            logger.info("Account creation rejected: username already exists username={}", username)
                        } else {
                            logger.warn("Account creation rejected for {}: missing Discord authorization", username)
                        }
                        val durationMs = (System.nanoTime() - startedAt) / 1_000_000L
                        onComplete.accept(LoginLoadResult(creationPreflightCode, durationMs, 0, null))
                        return@launch
                    }
                    val discordUser = if (accountCreationRequested) {
                        when (val discordResult =
                            net.dodian.uber.game.persistence.account.discord.DiscordApiService.fetchDiscordUserResult(discordAuthCode)
                        ) {
                            is net.dodian.uber.game.persistence.account.discord.DiscordFetchResult.Success ->
                                discordResult.user
                            net.dodian.uber.game.persistence.account.discord.DiscordFetchResult.InvalidAuthorization -> {
                                logger.warn("Account creation rejected for {}: invalid or expired Discord authorization", username)
                                val durationMs = (System.nanoTime() - startedAt) / 1_000_000L
                                onComplete.accept(LoginLoadResult(29, durationMs, 0, null))
                                return@launch
                            }
                            net.dodian.uber.game.persistence.account.discord.DiscordFetchResult.Unavailable -> {
                                logger.warn("Account creation unavailable for {}: Discord service/configuration failure", username)
                                val durationMs = (System.nanoTime() - startedAt) / 1_000_000L
                                onComplete.accept(LoginLoadResult(13, durationMs, 0, null))
                                return@launch
                            }
                        }
                    } else {
                        null
                    }
                    val email = discordUser?.email ?: ""
                    val discordId = discordUser?.id ?: ""
                    val discordUsername = discordUser?.username ?: ""
                    val discordVerified = discordUser?.verified ?: false
                    val deadline = System.currentTimeMillis() + 3_000L
                    var finalResult = AccountLoginService.PreparedLogin.failure(13)
                    while (true) {
                        val loadResult = AccountLoginService.prepareGame(
                            client,
                            username,
                            password,
                            email,
                            discordId,
                            discordUsername,
                            discordVerified,
                            accountCreationRequested = accountCreationRequested,
                        )
                        if (loadResult.code != AccountLoginService.FINAL_SAVE_PENDING_INTERNAL) {
                            finalResult = loadResult
                            break
                        }
                        pendingRetries++
                        if (System.currentTimeMillis() >= deadline) {
                            finalResult = AccountLoginService.PreparedLogin.failure(5)
                            break
                        }
                        delay(50L)
                    }
                    finalResult
                } catch (_: CancellationException) {
                    logger.info("Account load cancelled for {}", username)
                    AccountLoginService.PreparedLogin.failure(13)
                } catch (exception: RuntimeException) {
                    logger.warn("Account load failed for {}", username, exception)
                    AccountLoginService.PreparedLogin.failure(13)
                }
            val durationMs = (System.nanoTime() - startedAt) / 1_000_000L
            onComplete.accept(LoginLoadResult(prepared.code, durationMs, pendingRetries, prepared.takeIf { it.code == 0 }))
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

    internal fun accountCreationPreflightCode(
        accountCreationRequested: Boolean,
        discordAuthCode: String,
        usernameExists: () -> Boolean,
    ): Int? {
        if (!accountCreationRequested) return null
        if (discordAuthCode.isBlank()) return 29
        return if (usernameExists()) AccountLoginService.USERNAME_TAKEN else null
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
        TickThreadBlockingGuard.requireNotGameThread("AccountPersistenceService.saveSynchronously")
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
        TickThreadBlockingGuard.requireNotGameThread("AccountPersistenceService.shutdownAndDrain")
        scope.cancel("Account persistence shutdown")
        PlayerSaveService.shutdownAndDrain(timeout)
        DbDispatchers.shutdown(DbDispatchers.accountExecutor, timeout)
    }

    data class LoginLoadResult(
        val code: Int,
        val durationMs: Long,
        val pendingRetries: Int,
        val hydration: AccountLoginService.PreparedLogin? = null,
    )
}
