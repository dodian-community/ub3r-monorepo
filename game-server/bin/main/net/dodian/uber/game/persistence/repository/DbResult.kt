package net.dodian.uber.game.persistence.repository

sealed class DbResult<out T> {
    data class Success<T>(val value: T) : DbResult<T>()

    data class Failure(
        val error: Throwable,
        val retryable: Boolean,
    ) : DbResult<Nothing>()
}

