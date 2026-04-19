package net.dodian.uber.game.api.content

import java.sql.Connection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import net.dodian.uber.game.persistence.DbDispatchers
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import net.dodian.uber.game.persistence.repository.DbResult

object ContentPersistence {
    @JvmStatic
    fun fireAndForgetWrite(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher = DbDispatchers.accountDispatcher,
        write: () -> Unit,
    ) {
        DbAsyncRepository.fireAndForgetWrite(scope, dispatcher, write)
    }

    @JvmStatic
    fun fireAndForgetWriteConnection(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher = DbDispatchers.accountDispatcher,
        write: (Connection) -> Unit,
    ) {
        DbAsyncRepository.fireAndForgetWriteConnection(scope, dispatcher, write)
    }

    @JvmStatic
    suspend fun <T> read(
        dispatcher: CoroutineDispatcher = DbDispatchers.accountDispatcher,
        query: () -> T,
    ): DbResult<T> = DbAsyncRepository.suspendRead(dispatcher, query)

    @JvmStatic
    suspend fun <T> readConnection(
        dispatcher: CoroutineDispatcher = DbDispatchers.accountDispatcher,
        query: (Connection) -> T,
    ): DbResult<T> = DbAsyncRepository.suspendReadConnection(dispatcher, query)
}
