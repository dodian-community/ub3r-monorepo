package net.dodian.uber.game.runtime.scheduler

import net.dodian.uber.game.model.entity.npc.Npc

class NpcQueueTask(
    val npc: Npc,
    private val delegate: QueueTask,
) : QueueTask {
    override fun execute(): Boolean = delegate.execute()
}
