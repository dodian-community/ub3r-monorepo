package net.dodian.uber.net.protocol.packets.server.region

import net.dodian.uber.net.message.Message

abstract class RegionUpdateMessage : Message() {
    abstract val priority: Int

    companion object {
        const val HIGH_PRIORITY = 0
        const val LOW_PRIORITY = 1
    }
}