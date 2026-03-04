package net.dodian.uber.game.runtime.process

import net.dodian.jobs.impl.EntityProcessor

class NpcMainPhase(private val entityProcessor: EntityProcessor) {
    fun run(now: Long) {
        entityProcessor.runNpcMainPhase(now)
    }
}
