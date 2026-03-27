package net.dodian.uber.game.persistence.audit

import net.dodian.uber.game.model.YellSystem
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.db.dbConnection
import net.dodian.uber.game.config.gameWorldId
import org.slf4j.LoggerFactory

object CommandLog {
    private val logger = LoggerFactory.getLogger(CommandLog::class.java)
    private val insertSql =
        "INSERT INTO ${DbTables.GAME_LOGS_STAFF_COMMANDS} (userId, name, time, action) VALUES (?, ?, ?, ?)"

    @JvmStatic
    fun recordCommand(player: Player, command: String) {
        if (gameWorldId > 1 || player.playerGroup == 10) return

        AsyncSqlService.execute("command-log", Runnable {
            try {
                dbConnection.use { connection ->
                    connection.prepareStatement(insertSql).use { statement ->
                        statement.setInt(1, player.dbId)
                        statement.setString(2, player.playerName)
                        statement.setString(3, LogEntry.getTimeStamp())
                        statement.setString(4, "::${command.replace("'", "`")}")
                        statement.executeUpdate()
                    }
                }
            } catch (exception: Exception) {
                logger.error("Unable to record command", exception)
                YellSystem.alertStaff("Unable to record command logs, please contact an admin.")
            }
        })
    }
}
