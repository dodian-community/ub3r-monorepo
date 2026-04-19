package net.dodian.uber.game.persistence.audit

import java.sql.SQLException
import java.sql.Statement
import java.util.concurrent.CopyOnWriteArrayList
import net.dodian.uber.game.model.item.GameItem
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import net.dodian.uber.game.engine.config.gameWorldId
import net.dodian.uber.game.social.chat.YellSystem
import org.slf4j.LoggerFactory

object TradeLog {
    private val logger = LoggerFactory.getLogger(TradeLog::class.java)
    private val insertTradeSql =
        "INSERT INTO ${DbTables.GAME_LOGS_PLAYER_TRADES} (p1, p2, type, date) VALUES (?, ?, ?, ?)"
    private val insertItemSql =
        "INSERT INTO ${DbTables.GAME_LOGS_PLAYER} (id, pid, item, amount) VALUES (?, ?, ?, ?)"

    @JvmStatic
    fun recordTrade(
        p1: Int,
        p2: Int,
        items: CopyOnWriteArrayList<GameItem>,
        otherItems: CopyOnWriteArrayList<GameItem>,
        trade: Boolean,
    ) {
        if (gameWorldId > 1) return

        ConsoleAuditLog.trade(p1, p2, items, otherItems, trade)

        AsyncSqlService.execute("trade-log", Runnable {
            try {
                DbAsyncRepository.withConnection { connection ->
                    connection.prepareStatement(insertTradeSql, Statement.RETURN_GENERATED_KEYS).use { tradeStatement ->
                        tradeStatement.setInt(1, p1)
                        tradeStatement.setInt(2, p2)
                        tradeStatement.setInt(3, if (trade) 0 else 1)
                        tradeStatement.setString(4, LogEntry.getTimeStamp())
                        tradeStatement.executeUpdate()

                        tradeStatement.generatedKeys.use keys@{ generatedKeys ->
                            if (!generatedKeys.next()) return@keys
                            val logId = generatedKeys.getInt(1)
                            connection.prepareStatement(insertItemSql).use { itemStatement ->
                                addItems(itemStatement, logId, p1, items)
                                addItems(itemStatement, logId, p2, otherItems)
                                itemStatement.executeBatch()
                            }
                        }
                    }
                }
            } catch (exception: SQLException) {
                logger.error("Unable to record trade due to SQL exception", exception)
                YellSystem.alertStaff("Unable to record trade, please contact an admin.")
            } catch (exception: RuntimeException) {
                logger.error("Unable to record trade", exception)
                YellSystem.alertStaff("Unable to record trade, please contact an admin.")
            }
        })
    }

    private fun addItems(statement: java.sql.PreparedStatement, logId: Int, playerId: Int, items: Iterable<GameItem>) {
        for (item in items) {
            statement.setInt(1, logId)
            statement.setInt(2, playerId)
            statement.setInt(3, item.id)
            statement.setInt(4, item.amount)
            statement.addBatch()
        }
    }
}
