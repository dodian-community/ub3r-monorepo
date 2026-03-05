package net.dodian.uber.game.runtime.phases

import net.dodian.jobs.impl.OutboundPacketProcessor

class OutboundSyncPhase(private val outboundPacketProcessor: OutboundPacketProcessor) {
    fun run() {
        outboundPacketProcessor.run()
    }
}
