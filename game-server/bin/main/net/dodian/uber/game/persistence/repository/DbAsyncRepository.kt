package net.dodian.uber.game.persistence.repository

import java.sql.Connection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dodian.uber.game.persistence.DbDispatchers
import net.dodian.uber.game.persistence.db.dbConnection

object DbAsyncRepository {
    @JvmStatic
    inline fun <T> withConnection(query: (Connection) -> T): T {
        dbConnection.use { connection ->
            return query(connection)
        }
    }

    @JvmStatic
    fun fireAndForgetWrite(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher = DbDispatchers.accountDispatcher,
        write: () -> Unit,
    ) {
        scope.launch(dispatcher) {
            write()
        }
    }

    @JvmStatic
    suspend fun <T> suspendRead(
        dispatcher: CoroutineDispatcher = DbDispatchers.accountDispatcher,
        query: () -> T,
    ): DbResult<T> =
        withContext(dispatcher) {
            try {
                DbResult.Success(query())
            } catch (exception: Throwable) {
                DbResult.Failure(
                    error = exception,
                    retryable = false,
                )
            }
        }

    @JvmStatic
    fun fireAndForgetWriteConnection(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher = DbDispatchers.accountDispatcher,
        write: (Connection) -> Unit,
    ) {
        fireAndForgetWrite(scope, dispatcher) {
            dbConnection.use { connection ->
                write(connection)
            }
        }
    }

    @JvmStatic
    suspend fun <T> suspendReadConnection(
        dispatcher: CoroutineDispatcher = DbDispatchers.accountDispatcher,
        query: (Connection) -> T,
    ): DbResult<T> =
        suspendRead(dispatcher) {
            dbConnection.use { connection ->
                query(connection)
            }
        }
}
