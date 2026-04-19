package net.dodian.uber.game.engine.scheduler

import net.dodian.uber.game.model.entity.npc.Npc

class NpcQueueTask(
    val npc: Npc,
    private val delegate: QueueTask,
) : QueueTask {
    override fun execute(): Boolean = delegate.execute()
}
