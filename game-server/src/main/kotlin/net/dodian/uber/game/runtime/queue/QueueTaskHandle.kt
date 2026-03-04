package net.dodian.uber.game.runtime.queue

class QueueTaskHandle internal constructor(
    private val token: QueueCancellationToken,
) {
    fun cancel() {
        token.cancel()
    }
}
