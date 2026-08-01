package net.dodian.uber.game.persistence.account.login

import java.sql.Connection
import java.sql.SQLException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.persistence.account.AccountPersistenceService
import net.dodian.uber.game.persistence.account.Login
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import net.dodian.uber.game.engine.config.discordMaxAccountsPerDiscord
import net.dodian.uber.game.engine.config.discordRequireVerifiedEmail
import net.dodian.uber.game.engine.config.rankBannedGroupId
import net.dodian.uber.game.engine.config.rankDebugBypassGroupIds
import net.dodian.uber.game.engine.config.serverDebugMode
import net.dodian.uber.game.engine.config.serverEnv
import net.dodian.uber.game.engine.loop.GameThreadContext
import org.slf4j.LoggerFactory

object AccountLoginService {
    private val logger = LoggerFactory.getLogger(AccountLoginService::class.java)
    const val FINAL_SAVE_PENDING_INTERNAL = 98
    const val USERNAME_TAKEN = 33

    @JvmStatic
    @JvmOverloads
    fun prepareGame(
        player: Client,
        playerName: String,
        playerPass: String,
        email: String = "",
        discordId: String = "",
        discordUsername: String = "",
        discordVerified: Boolean = false,
        accountCreationRequested: Boolean = false,
    ): PreparedLogin {
        val prepared = try {
            DbAsyncRepository.withConnection { connection ->
                prepareGame(
                    player = player,
                    playerName = playerName,
                    playerPass = playerPass,
                    email = email,
                    discordId = discordId,
                    discordUsername = discordUsername,
                    discordVerified = discordVerified,
                    accountCreationRequested = accountCreationRequested,
                    connection = connection,
                    allowDevAutoCreate = isDevAutoCreateEnabled() && isLoopback(player),
                    allowDevPasswordBypass = isDevPasswordBypassEnabled(),
                )
            }
        } catch (exception: SQLException) {
            logger.error("Critical SQL error while loading player {}", playerName, exception)
            PreparedLogin.failure(13)
        } catch (exception: RuntimeException) {
            logger.error("Critical runtime error while loading player {}", playerName, exception)
            PreparedLogin.failure(13)
        }

        if (prepared.code == 0) {
            net.dodian.uber.game.engine.metrics.PrometheusMetricsRegistry.recordLogin("success")
        } else {
            net.dodian.uber.game.engine.metrics.PrometheusMetricsRegistry.recordLogin("failed")
        }
        return prepared
    }

    @JvmStatic
    fun updatePlayerForumRegistration(player: Client) {
        try {
            DbAsyncRepository.withConnection { connection ->
                AccountLoginRepository.updateForumRegistration(connection, player.dbId, "40")
            }
        } catch (exception: SQLException) {
            logger.error("Failed to update forum rights for dbId={}", player.dbId, exception)
        } catch (exception: RuntimeException) {
            logger.error("Unexpected runtime error while updating forum rights for dbId={}", player.dbId, exception)
        }
        player.sendMessage("You have now been registered to the forum! Enjoy your stay :D")
    }

    @JvmStatic
    fun isBanned(id: Int): Boolean =
        DbAsyncRepository.withConnection { connection ->
            AccountLoginRepository.isBanned(connection, id)
        }

    /** Fast preflight used before exchanging a one-time Discord OAuth code. */
    @JvmStatic
    fun usernameExists(playerName: String): Boolean =
        DbAsyncRepository.withConnection { connection ->
            AccountLoginRepository.loadWebUser(connection, playerName) != null
        }

    internal fun prepareGame(
        player: Client,
        playerName: String,
        playerPass: String,
        email: String = "",
        discordId: String = "",
        discordUsername: String = "",
        discordVerified: Boolean = false,
        accountCreationRequested: Boolean = false,
        connection: Connection,
        allowDevAutoCreate: Boolean,
        allowDevPasswordBypass: Boolean,
    ): PreparedLogin {
        if (PlayerRegistry.isPlayerOn(playerName)) return PreparedLogin.failure(5)
        if (playerName.isEmpty()) return PreparedLogin.failure(3)

        var webUser = AccountLoginRepository.loadWebUser(connection, playerName)
        if (webUser == null) {
            if (!accountCreationRequested) {
                logger.info("Login rejected for {}: account does not exist and creation was not requested", playerName)
                return PreparedLogin.failure(26)
            }
            // Mandatory: new accounts require a verified Discord identity (a non-blank email means
            // AccountPersistenceService already exchanged a valid discordAuthCode for it), in any environment.
            // The one exception is local dev testing (SERVER_ENV=dev + debug=true), which can create a
            // throwaway account without going through Discord.
            if (email.isBlank() && !allowDevAutoCreate) {
                logger.warn("Account creation rejected for {}: no Discord account was supplied, existing user must create an account first", playerName)
                return PreparedLogin.failure(26)
            }
            if (playerName.length < 3) {
                logger.warn("Account creation rejected for {}: username shorter than 3 characters", playerName)
                return PreparedLogin.failure(23)
            }
            // A verified Discord identity must include the Discord user id, not just an email -
            // without this, a Success response missing the id would fall through to the
            // unlimited insertWebUser branch below and bypass the per-Discord account cap.
            if (discordId.isBlank() && !allowDevAutoCreate) {
                logger.warn("Account creation rejected for {}: Discord identity missing user id", playerName)
                return PreparedLogin.failure(26)
            }
            if (discordId.isNotBlank()) {
                if (discordRequireVerifiedEmail && !discordVerified) {
                    logger.warn("Account creation rejected for discord_id {}: Discord email is not verified", discordId)
                    return PreparedLogin.failure(32)
                }
                val maxAllowed = discordMaxAccountsPerDiscord
                when (AccountLoginRepository.insertWebUserIfUnderDiscordLimit(
                    connection, playerName, playerPass, email, discordId, discordUsername, maxAllowed,
                )) {
                    AccountLoginRepository.AccountCreationInsertResult.INSERTED -> Unit
                    AccountLoginRepository.AccountCreationInsertResult.DISCORD_LIMIT_REACHED -> {
                        logger.warn("Account creation rejected for discord_id {}: limit of {} accounts reached", discordId, maxAllowed)
                        return PreparedLogin.failure(31)
                    }
                    AccountLoginRepository.AccountCreationInsertResult.USERNAME_TAKEN -> {
                        logger.info("Account creation rejected after concurrent username collision username={}", playerName)
                        return PreparedLogin.failure(USERNAME_TAKEN)
                    }
                }
            } else {
                AccountLoginRepository.insertWebUser(connection, playerName, playerPass, email, discordId, discordUsername)
            }
            webUser = AccountLoginRepository.loadWebUser(connection, playerName) ?: return PreparedLogin.failure(13)
        } else if (accountCreationRequested) {
            // A Discord authorization code explicitly means this is the Create Account flow, not
            // an ordinary login. Never attach it to or log into an existing username.
            logger.info("Account creation rejected: username already exists username={}", playerName)
            return PreparedLogin.failure(USERNAME_TAKEN)
        }
        if (AccountPersistenceService.isFinalSavePending(webUser.dbId)) {
            return PreparedLogin.failure(FINAL_SAVE_PENDING_INTERNAL)
        }
        if (!webUser.username.equals(playerName, ignoreCase = true)) return PreparedLogin.failure(30)
        val hashedPassword = Client.passHash(playerPass, webUser.salt)
        if (!constantTimeEquals(hashedPassword, webUser.password) &&
            !isDebugPasswordBypassAllowed(player, webUser.playerGroup, allowDevPasswordBypass)
        ) {
            return PreparedLogin.failure(3)
        }
        if (webUser.playerGroup == rankBannedGroupId) return PreparedLogin.failure(4)

        val character = AccountLoginRepository.loadCharacter(connection, webUser.dbId)
        val newCharacter: Boolean
        if (character == null) {
            AccountLoginRepository.createCharacter(connection, webUser.dbId, playerName)
            newCharacter = true
        } else {
            if (System.currentTimeMillis() < character.unbanTime) return PreparedLogin.failure(4)
            if (Login.isUidBanned(player.UUID)) return PreparedLogin.failure(22)
            if (!character.statsPresent) {
                AccountLoginRepository.backfillMissingStats(connection, webUser.dbId)
            }
            newCharacter = false
        }
        return PreparedLogin(
            code = 0,
            dbId = webUser.dbId,
            playerGroup = webUser.playerGroup,
            otherGroups = webUser.otherGroups.toList(),
            unreadPmCount = webUser.unreadPmCount,
            newCharacter = newCharacter,
            character = character,
        )
    }

    @JvmStatic
    fun hydrateGame(player: Client, prepared: PreparedLogin) {
        GameThreadContext.requireGameThread("account.login.hydrate")
        check(prepared.code == 0) { "Cannot hydrate failed login code ${prepared.code}" }
        player.dbId = prepared.dbId
        player.playerGroup = prepared.playerGroup
        player.otherGroups = prepared.otherGroups.toTypedArray()
        player.newPms = prepared.unreadPmCount
        if (prepared.newCharacter) {
            AccountLoginMapper.applyNewCharacterDefaults(player)
        } else {
            AccountLoginMapper.applyExistingCharacter(player, checkNotNull(prepared.character))
        }
        val now = System.currentTimeMillis()
        player.lastSave = now
        player.start = now
        player.loadingDone = true
    }

    private fun isDevAutoCreateEnabled(): Boolean = serverEnv == "dev" && serverDebugMode

    private fun isLoopback(player: Client): Boolean =
        player.connectedFrom == "127.0.0.1" || player.connectedFrom == "0:0:0:0:0:0:0:1"

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(
            left.lowercase().toByteArray(StandardCharsets.US_ASCII),
            right.lowercase().toByteArray(StandardCharsets.US_ASCII),
        )

    // Both dev bypasses require SERVER_ENV=dev AND debug=true, matching the startup banner's
    // documented contract ("dev allows them only from localhost/debug-bypass ranks and only
    // with debug=true"). Previously the localhost password bypass worked in dev without debug.
    private fun isDevPasswordBypassEnabled(): Boolean = serverEnv == "dev" && serverDebugMode

    private fun isDebugPasswordBypassAllowed(
        player: Client,
        playerGroup: Int,
        allowDevPasswordBypass: Boolean,
    ): Boolean {
        if (!allowDevPasswordBypass) {
            return false
        }
        return isLoopback(player) || rankDebugBypassGroupIds.contains(playerGroup)
    }

    data class PreparedLogin(
        val code: Int,
        val dbId: Int = -1,
        val playerGroup: Int = 3,
        val otherGroups: List<String> = emptyList(),
        val unreadPmCount: Int = 0,
        val newCharacter: Boolean = false,
        val character: AccountLoginRepository.JoinedCharacterRow? = null,
    ) {
        companion object {
            @JvmStatic fun failure(code: Int): PreparedLogin = PreparedLogin(code = code)
        }
    }
}
