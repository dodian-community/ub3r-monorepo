package net.dodian.uber.game.persistence.player

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import net.dodian.uber.game.persistence.db.dbConnection

class PlayerSaveRepository(
    private val connectionProvider: ConnectionProvider = ConnectionProvider { defaultConnection() },
) {
    data class Statement(
        val sql: String,
        val values: List<Any?>,
        val expectedUpdateCount: Int? = null,
    )
    fun interface ConnectionProvider {
        @Throws(SQLException::class)
        fun getConnection(): Connection
    }

    @Throws(SQLException::class)
    @Deprecated("Raw SQL snapshots are not executable; use savePrepared.", level = DeprecationLevel.ERROR)
    fun saveSnapshot(snapshot: PlayerSaveSnapshot) {
        throw UnsupportedOperationException("Raw player-save SQL snapshots are not executable")
    }

    @Throws(SQLException::class)
    fun savePrepared(statements: List<Statement>) {
        connectionProvider.getConnection().use { connection ->
            val originalAutoCommit = connection.autoCommit
            connection.autoCommit = false
            try {
                statements.forEach { command ->
                    connection.prepareStatement(command.sql).use { statement ->
                        command.values.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
                        val updated = statement.executeUpdate()
                        command.expectedUpdateCount?.let { expected ->
                            if (updated != expected) {
                                throw SQLException(
                                    "Player save integrity check failed: expected $expected updated row(s), got $updated",
                                )
                            }
                        }
                    }
                }
                connection.commit()
            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            } finally {
                connection.autoCommit = originalAutoCommit
            }
        }
    }

    companion object {
        @JvmStatic
        @Throws(SQLException::class)
        fun defaultConnection(): Connection = dbConnection
    }
}
