package net.dodian.uber.game.runtime.scheduler

class WorldQueueTask(
    private val delegate: QueueTask,
) : QueueTask {
    override fun execute(): Boolean = delegate.execute()
}
