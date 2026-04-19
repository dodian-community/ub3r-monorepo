package net.dodian.uber.game.persistence.player

import java.sql.Timestamp
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.repository.DbAsyncRepository

data class RefundRecord(
    val date: String,
    val itemId: Int,
    val amount: Int,
)

object RefundRepository {
    @JvmStatic
    fun loadUnclaimed(receivedBy: Int): List<RefundRecord> =
        DbAsyncRepository.withConnection { connection ->
            connection
                .prepareStatement(
                    "SELECT date, item, amount FROM ${DbTables.GAME_REFUND_ITEMS} WHERE receivedBy = ? AND claimed IS NULL ORDER BY date ASC",
                ).use { statement ->
                    statement.setInt(1, receivedBy)
                    statement.executeQuery().use { results ->
                        val refunds = ArrayList<RefundRecord>()
                        while (results.next()) {
                            refunds += RefundRecord(results.getString("date"), results.getInt("item"), results.getInt("amount"))
                        }
                        refunds
                    }
                }
        }

    @JvmStatic
    fun markClaimed(receivedBy: Int, refundDate: String): Boolean =
        DbAsyncRepository.withConnection { connection ->
            connection
                .prepareStatement(
                    "UPDATE ${DbTables.GAME_REFUND_ITEMS} SET claimed = ? WHERE receivedBy = ? AND date = ? AND claimed IS NULL",
                ).use { statement ->
                    statement.setTimestamp(1, Timestamp(System.currentTimeMillis()))
                    statement.setInt(2, receivedBy)
                    statement.setString(3, refundDate)
                    statement.executeUpdate() > 0
                }
        }
}
