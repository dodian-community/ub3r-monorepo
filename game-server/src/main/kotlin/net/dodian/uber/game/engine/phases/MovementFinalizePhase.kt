package net.dodian.uber.game.engine.phases

import net.dodian.uber.game.engine.processing.EntityProcessor

class MovementFinalizePhase(private val entityProcessor: EntityProcessor) {
    fun run() {
        entityProcessor.runMovementFinalizePhase()
    }
}
