package net.dodian.uber.game.persistence.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dodian.uber.game.persistence.DbDispatchers

object DbAsyncRepository {
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
}

