package net.dodian.uber.game.persistence.audit

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.YellSystem
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import net.dodian.uber.game.engine.config.gameWorldId
import org.slf4j.LoggerFactory

object ItemLog {
    private val logger = LoggerFactory.getLogger(ItemLog::class.java)
    private val insertSql =
        "INSERT INTO ${DbTables.GAME_LOGS_ITEMS} (receiver, type, from_id, item_id, item_amount, timestamp, x, y, z, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

    @JvmStatic
    fun playerPickup(player: Player, userId: Int, itemId: Int, itemAmount: Int, pos: Position, npc: Boolean) {
        record(player.dbId, 1, userId, itemId, itemAmount, pos, if (npc) "npc" else "player", "item-log-pickup", "Unable to record player pickup of a item, please contact an admin.", "Unable to record player picking up items")
    }

    @JvmStatic
    fun playerDrop(player: Player, itemId: Int, itemAmount: Int, pos: Position, reason: String) {
        val dropReason = if (reason.isEmpty()) "player" else reason
        record(player.dbId, 2, -1, itemId, itemAmount, pos, dropReason, "item-log-drop", "Unable to record player drop of a item, please contact an admin.", "Unable to record player dropping items")
    }

    @JvmStatic
    fun npcDrop(player: Player, npcId: Int, itemId: Int, itemAmount: Int, pos: Position) {
        record(player.dbId, 2, npcId, itemId, itemAmount, pos, "npc", "item-log-npc-drop", "Unable to record npc drop of a item, please contact an admin.", "Unable to record npc dropping items")
    }

    @JvmStatic
    fun playerGathering(player: Player, itemId: Int, itemAmount: Int, pos: Position, reason: String) {
        record(player.dbId, 3, -1, itemId, itemAmount, pos, reason, "item-log-gathering", "Unable to record player pickup of a item, please contact an admin.", "Unable to record player gathering items")
    }

    private fun record(
        receiverId: Int,
        type: Int,
        fromId: Int,
        itemId: Int,
        itemAmount: Int,
        pos: Position,
        reason: String,
        taskName: String,
        alertMessage: String,
        errorMessage: String,
    ) {
        if (gameWorldId > 1) return

        AsyncSqlService.execute(taskName, Runnable {
            try {
                DbAsyncRepository.withConnection { connection ->
                    connection.prepareStatement(insertSql).use { statement ->
                        statement.setInt(1, receiverId)
                        statement.setInt(2, type)
                        statement.setInt(3, fromId)
                        statement.setInt(4, itemId)
                        statement.setInt(5, itemAmount)
                        statement.setString(6, LogEntry.getTimeStamp())
                        statement.setInt(7, pos.x)
                        statement.setInt(8, pos.y)
                        statement.setInt(9, pos.z)
                        statement.setString(10, reason)
                        statement.executeUpdate()
                    }
                }
            } catch (exception: Exception) {
                logger.error(errorMessage, exception)
                YellSystem.alertStaff(alertMessage)
            }
        })
    }
}
