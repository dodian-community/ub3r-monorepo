package net.dodian.uber.game.persistence.audit

import java.sql.SQLException
import net.dodian.uber.game.systems.chat.YellSystem
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import net.dodian.uber.game.engine.config.gameWorldId
import org.slf4j.LoggerFactory

object DuelLog {
    private val logger = LoggerFactory.getLogger(DuelLog::class.java)
    private val insertSql =
        "INSERT INTO ${DbTables.GAME_LOGS_PLAYER_DUELS} (player, opponent, playerstake, opponentstake, winner, timestamp) VALUES (?, ?, ?, ?, ?, ?)"

    @JvmStatic
    fun recordDuel(player: String, opponent: String, playerStake: String, opponentStake: String, winner: String) {
        if (gameWorldId > 1) return

        ConsoleAuditLog.duel(player, opponent, playerStake, opponentStake, winner)

        AsyncSqlService.execute("duel-log", Runnable {
            try {
                DbAsyncRepository.withConnection { connection ->
                    connection.prepareStatement(insertSql).use { statement ->
                        statement.setString(1, player)
                        statement.setString(2, opponent)
                        statement.setString(3, playerStake)
                        statement.setString(4, opponentStake)
                        statement.setString(5, winner)
                        statement.setString(6, LogEntry.getTimeStamp())
                        statement.executeUpdate()
                    }
                }
            } catch (exception: SQLException) {
                logger.error("Unable to record duel due to SQL exception", exception)
                YellSystem.alertStaff("Unable to record duels, please contact an admin.")
            } catch (exception: RuntimeException) {
                logger.error("Unable to record duel", exception)
                YellSystem.alertStaff("Unable to record duels, please contact an admin.")
            }
        })
    }
}
