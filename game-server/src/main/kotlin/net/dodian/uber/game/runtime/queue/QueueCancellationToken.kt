package net.dodian.uber.game.runtime.queue

class QueueCancellationToken {
    @Volatile
    var cancelled: Boolean = false
        private set

    fun cancel() {
        cancelled = true
    }
}
