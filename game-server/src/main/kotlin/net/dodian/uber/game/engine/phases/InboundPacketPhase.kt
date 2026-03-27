package net.dodian.uber.game.engine.phases

import net.dodian.jobs.impl.EntityProcessor

class InboundPacketPhase(private val entityProcessor: EntityProcessor) {
    fun run() {
        entityProcessor.runInboundPacketPhase()
    }
}
