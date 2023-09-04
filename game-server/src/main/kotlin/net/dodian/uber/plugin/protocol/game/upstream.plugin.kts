package net.dodian.uber.plugin.protocol.game

import net.dodian.uber.plugin.context
import net.dodian.uber.protocol.upstream.ClickButton

val packets = context().packetMap.upstream

packets.register<ClickButton> {
    opcode = 185
    decode { buf ->
        val interfaceId = buf.readUnsignedShort()

        ClickButton(interfaceId)
    }
}