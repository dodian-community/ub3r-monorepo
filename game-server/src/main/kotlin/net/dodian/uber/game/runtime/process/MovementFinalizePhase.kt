package net.dodian.uber.game.runtime.process

import net.dodian.jobs.impl.EntityProcessor

class MovementFinalizePhase(private val entityProcessor: EntityProcessor) {
    fun run() {
        entityProcessor.runMovementFinalizePhase()
    }
}
