package net.dodian.uber.game.persistence.account.login

import java.sql.Connection
import java.sql.SQLException
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.persistence.account.AccountPersistenceService
import net.dodian.uber.game.persistence.account.Login
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import net.dodian.uber.game.engine.config.discordMaxAccountsPerDiscord
import net.dodian.uber.game.engine.config.rankBannedGroupId
import net.dodian.uber.game.engine.config.rankDebugBypassGroupIds
import net.dodian.uber.game.engine.config.serverDebugMode
import net.dodian.uber.game.engine.config.serverEnv
import net.dodian.uber.game.engine.loop.GameThreadContext
import org.slf4j.LoggerFactory

object AccountLoginService {
    private val logger = LoggerFactory.getLogger(AccountLoginService::class.java)
    const val FINAL_SAVE_PENDING_INTERNAL = 98

    @JvmStatic
    @JvmOverloads
    fun prepareGame(player: Client, playerName: String, playerPass: String, email: String = ""): PreparedLogin {
        val prepared = try {
            DbAsyncRepository.withConnection { connection ->
                prepareGame(
                    player = player,
                    playerName = playerName,
                    playerPass = playerPass,
                    email = email,
                    connection = connection,
                    allowDevAutoCreate = isDevAutoCreateEnabled(),
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

    internal fun prepareGame(
        player: Client,
        playerName: String,
        playerPass: String,
        email: String = "",
        connection: Connection,
        allowDevAutoCreate: Boolean,
        allowDevPasswordBypass: Boolean,
    ): PreparedLogin {
        if (PlayerRegistry.isPlayerOn(playerName)) return PreparedLogin.failure(5)
        if (playerName.isEmpty()) return PreparedLogin.failure(3)

        var webUser = AccountLoginRepository.loadWebUser(connection, playerName)
        if (webUser == null) {
            if (!allowDevAutoCreate) return PreparedLogin.failure(12)
            // Mandatory: new accounts require a verified Discord identity (a non-blank email means
            // AccountPersistenceService already exchanged a valid discordAuthCode for it). A blank/missing/
            // invalid code must never be able to create an account, in any environment.
            if (email.isBlank()) {
                logger.warn("Account creation rejected for {}: no verified Discord account was supplied", playerName)
                return PreparedLogin.failure(13)
            }
            val count = AccountLoginRepository.countAccountsByEmail(connection, email)
            val maxAllowed = discordMaxAccountsPerDiscord
            if (count >= maxAllowed) {
                logger.warn("Account creation rejected for email {}: limit of {} accounts reached", email, maxAllowed)
                return PreparedLogin.failure(12)
            }
            AccountLoginRepository.insertWebUser(connection, playerName, playerPass, email)
            webUser = AccountLoginRepository.loadWebUser(connection, playerName) ?: return PreparedLogin.failure(13)
        } else if (webUser.password.isEmpty() && playerPass.isNotEmpty()) {
            AccountLoginRepository.updateWebUserCredentials(connection, webUser.dbId, playerPass, email)
            webUser = AccountLoginRepository.loadWebUser(connection, playerName) ?: return PreparedLogin.failure(13)
        }
        if (AccountPersistenceService.isFinalSavePending(webUser.dbId)) {
            return PreparedLogin.failure(FINAL_SAVE_PENDING_INTERNAL)
        }
        if (!webUser.username.equals(playerName, ignoreCase = true)) return PreparedLogin.failure(12)
        val hashedPassword = Client.passHash(playerPass, webUser.salt)
        if (hashedPassword != webUser.password &&
            !isDebugPasswordBypassAllowed(player, webUser.playerGroup, allowDevPasswordBypass)
        ) {
            return PreparedLogin.failure(3)
        }
        if (webUser.playerGroup == rankBannedGroupId) return PreparedLogin.failure(12)

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

    private fun isDevPasswordBypassEnabled(): Boolean = serverEnv == "dev"

    private fun isDebugPasswordBypassAllowed(
        player: Client,
        playerGroup: Int,
        allowDevPasswordBypass: Boolean,
    ): Boolean {
        if (!allowDevPasswordBypass) {
            return false
        }
        return player.connectedFrom == "127.0.0.1" ||
            (serverDebugMode && rankDebugBypassGroupIds.contains(playerGroup))
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
