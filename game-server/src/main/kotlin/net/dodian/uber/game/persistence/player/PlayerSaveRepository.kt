package net.dodian.uber.game.persistence.player

import java.sql.Connection
import java.sql.SQLException
import net.dodian.uber.game.persistence.db.dbConnection

class PlayerSaveRepository(
    private val connectionProvider: ConnectionProvider = ConnectionProvider { defaultConnection() },
) {
    fun interface ConnectionProvider {
        @Throws(SQLException::class)
        fun getConnection(): Connection
    }

    @Throws(SQLException::class)
    fun saveSnapshot(snapshot: PlayerSaveSnapshot) {
        connectionProvider.getConnection().use { connection ->
            val originalAutoCommit = connection.autoCommit
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(snapshot.statsUpdateSql)
                    snapshot.statsProgressInsertSql?.let(statement::executeUpdate)
                    statement.executeUpdate(snapshot.characterUpdateSql)
                    connection.commit()
                }
            } catch (sqlException: SQLException) {
                connection.rollback()
                throw sqlException
            } catch (runtimeException: RuntimeException) {
                connection.rollback()
                throw runtimeException
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
