package net.dodian.uber.game.engine.phases

import net.dodian.uber.game.engine.processing.EntityProcessor

class PlayerMainPhase(private val entityProcessor: EntityProcessor) {
    fun run() {
        entityProcessor.runPlayerMainPhase(System.currentTimeMillis())
    }
}
