package net.dodian.uber.game.engine.phases

import net.dodian.uber.game.engine.processing.EntityProcessor

class InboundPacketPhase(private val entityProcessor: EntityProcessor) {
    fun run() {
        entityProcessor.runInboundPacketPhase()
    }
}
