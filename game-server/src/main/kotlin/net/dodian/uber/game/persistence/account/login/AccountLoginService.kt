package net.dodian.uber.game.persistence.account.login

import java.sql.Connection
import java.sql.SQLException
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.account.AccountPersistenceService
import net.dodian.uber.game.persistence.account.Login
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import net.dodian.uber.game.engine.config.serverDebugMode
import net.dodian.uber.game.engine.config.serverEnv
import org.slf4j.LoggerFactory

object AccountLoginService {
    private val logger = LoggerFactory.getLogger(AccountLoginService::class.java)
    const val FINAL_SAVE_PENDING_INTERNAL = 98

    @JvmStatic
    fun loadCharacterGame(player: Client, playerName: String, playerPass: String): Int =
        try {
            DbAsyncRepository.withConnection { connection ->
                loadCharacterGame(
                    player = player,
                    playerName = playerName,
                    playerPass = playerPass,
                    connection = connection,
                    allowDevAutoCreate = isDevAutoCreateEnabled(),
                    allowDevPasswordBypass = isDevPasswordBypassEnabled(),
                )
            }
        } catch (exception: SQLException) {
            logger.error("Failed to load player {} due to SQL exception", playerName, exception)
            13
        } catch (exception: RuntimeException) {
            logger.error("Failed to load player {} due to runtime exception", playerName, exception)
            13
        }

    @JvmStatic
    fun loadGame(player: Client, playerName: String, playerPass: String): Int =
        try {
            DbAsyncRepository.withConnection { connection ->
                loadGame(
                    player = player,
                    playerName = playerName,
                    playerPass = playerPass,
                    connection = connection,
                    allowDevAutoCreate = isDevAutoCreateEnabled(),
                    allowDevPasswordBypass = isDevPasswordBypassEnabled(),
                )
            }
        } catch (exception: SQLException) {
            logger.error("Critical SQL error while loading player {}", playerName, exception)
            13
        } catch (exception: RuntimeException) {
            logger.error("Critical runtime error while loading player {}", playerName, exception)
            13
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

    internal fun loadCharacterGame(
        player: Client,
        playerName: String,
        playerPass: String,
        connection: Connection,
        allowDevAutoCreate: Boolean,
        allowDevPasswordBypass: Boolean,
    ): Int {
        if (PlayerRegistry.isPlayerOn(playerName)) {
            return 5
        }
        if (playerName.isEmpty()) {
            return 3
        }

        val webUser = AccountLoginRepository.loadWebUser(connection, playerName)
        if (webUser == null) {
            if (!allowDevAutoCreate) {
                return 12
            }
            AccountLoginRepository.insertWebUser(connection, playerName)
            return loadCharacterGame(player, playerName, playerPass, connection, false, allowDevPasswordBypass)
        }

        player.dbId = webUser.dbId
        if (AccountPersistenceService.isFinalSavePending(player.dbId)) {
            return FINAL_SAVE_PENDING_INTERNAL
        }
        player.playerGroup = webUser.playerGroup
        player.otherGroups = webUser.otherGroups

        if (!webUser.username.equals(playerName, ignoreCase = true)) {
            return 12
        }

        val hashedPassword = Client.passHash(playerPass, webUser.salt)
        if (hashedPassword != webUser.password && !isDebugPasswordBypassAllowed(player, allowDevPasswordBypass)) {
            return 3
        }

        player.newPms = webUser.unreadPmCount
        return 0
    }

    internal fun loadGame(
        player: Client,
        playerName: String,
        playerPass: String,
        connection: Connection,
        allowDevAutoCreate: Boolean,
        allowDevPasswordBypass: Boolean,
    ): Int {
        val loadCharacterResponse =
            loadCharacterGame(player, playerName, playerPass, connection, allowDevAutoCreate, allowDevPasswordBypass)
        if (loadCharacterResponse > 0) {
            return loadCharacterResponse
        }
        if (player.playerGroup == 3) {
            return 12
        }

        val character = AccountLoginRepository.loadCharacter(connection, player.dbId)
        if (character == null) {
            AccountLoginRepository.createCharacter(connection, player.dbId, playerName)
            AccountLoginMapper.applyNewCharacterDefaults(player)
        } else {
            if (System.currentTimeMillis() < character.unbanTime) {
                return 4
            }
            if (Login.isUidBanned(player.UUID)) {
                return 22
            }
            if (!character.statsPresent) {
                AccountLoginRepository.backfillMissingStats(connection, player.dbId)
            }
            AccountLoginMapper.applyExistingCharacter(player, character)
        }

        player.lastSave = System.currentTimeMillis()
        player.start = System.currentTimeMillis()
        player.loadingDone = true
        return 0
    }

    private fun isDevAutoCreateEnabled(): Boolean = serverEnv == "dev" && serverDebugMode

    private fun isDevPasswordBypassEnabled(): Boolean = serverEnv == "dev"

    private fun isDebugPasswordBypassAllowed(player: Client, allowDevPasswordBypass: Boolean): Boolean {
        if (!allowDevPasswordBypass) {
            return false
        }
        return player.connectedFrom == "127.0.0.1" ||
            (serverDebugMode && (player.playerGroup == 40 || player.playerGroup == 34 || player.playerGroup == 11))
    }
}
