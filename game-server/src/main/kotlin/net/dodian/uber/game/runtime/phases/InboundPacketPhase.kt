package net.dodian.uber.game.runtime.phases

import net.dodian.jobs.impl.EntityProcessor

class InboundPacketPhase(private val entityProcessor: EntityProcessor) {
    fun run() {
        entityProcessor.runInboundPacketPhase()
    }
}
