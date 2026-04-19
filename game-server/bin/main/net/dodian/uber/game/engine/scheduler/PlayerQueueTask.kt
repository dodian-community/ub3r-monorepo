package net.dodian.uber.game.engine.scheduler

import net.dodian.uber.game.model.entity.player.Client

class PlayerQueueTask(
    val player: Client,
    private val delegate: QueueTask,
) : QueueTask {
    override fun execute(): Boolean = delegate.execute()
}
