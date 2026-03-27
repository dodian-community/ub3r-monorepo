package net.dodian.uber.game.persistence.account.login

import java.sql.Connection
import net.dodian.uber.game.model.Login
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.account.AccountPersistenceService
import net.dodian.uber.game.persistence.db.dbConnection
import net.dodian.uber.game.config.serverDebugMode
import net.dodian.uber.game.config.serverEnv

object AccountLoginService {
    const val FINAL_SAVE_PENDING_INTERNAL = 98

    @JvmStatic
    fun loadCharacterGame(player: Client, playerName: String, playerPass: String): Int =
        try {
            dbConnection.use { connection ->
                loadCharacterGame(
                    player = player,
                    playerName = playerName,
                    playerPass = playerPass,
                    connection = connection,
                    allowDevAutoCreate = isDevAutoCreateEnabled(),
                    allowDevPasswordBypass = isDevPasswordBypassEnabled(),
                )
            }
        } catch (exception: Exception) {
            println("Failed to load player: $playerName, $exception")
            13
        }

    @JvmStatic
    fun loadGame(player: Client, playerName: String, playerPass: String): Int =
        try {
            dbConnection.use { connection ->
                loadGame(
                    player = player,
                    playerName = playerName,
                    playerPass = playerPass,
                    connection = connection,
                    allowDevAutoCreate = isDevAutoCreateEnabled(),
                    allowDevPasswordBypass = isDevPasswordBypassEnabled(),
                )
            }
        } catch (exception: Exception) {
            println("A critical error occurred while loading player: $playerName. Exception: $exception")
            exception.printStackTrace()
            13
        }

    @JvmStatic
    fun updatePlayerForumRegistration(player: Client) {
        try {
            dbConnection.use { connection ->
                AccountLoginRepository.updateForumRegistration(connection, player.dbId, "40")
            }
        } catch (exception: Exception) {
            println("Something wrong with updating a players forum rights $exception")
        }
        player.send(SendMessage("You have now been registered to the forum! Enjoy your stay :D"))
    }

    @JvmStatic
    fun isBanned(id: Int): Boolean =
        dbConnection.use { connection ->
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
        if (PlayerHandler.isPlayerOn(playerName)) {
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
