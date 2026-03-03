package net.dodian.uber.game.runtime.queue

class WorldQueueTask(
    private val delegate: QueueTask,
) : QueueTask {
    override fun execute(): Boolean = delegate.execute()
}
