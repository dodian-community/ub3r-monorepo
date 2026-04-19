package net.dodian.uber.game.engine.phases

import net.dodian.uber.game.engine.processing.EntityProcessor

class NpcMainPhase(private val entityProcessor: EntityProcessor) {
    fun run(now: Long) {
        entityProcessor.runNpcMainPhase(now)
    }
}
