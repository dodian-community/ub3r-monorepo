package net.dodian.uber.plugin.protocol.game

import net.dodian.uber.plugin.context
import net.dodian.uber.protocol.upstream.ClickButton
import net.dodian.uber.protocol.upstream.KeepAlive

val packets = context().packetMap.upstream

packets.register<ClickButton> {
    opcode = 185
    decode { buf ->
        val interfaceId = buf.readUnsignedShort()

        ClickButton(interfaceId)
    }
}

packets.register<KeepAlive> {
    opcode = 0
    decode {
        KeepAlive()
    }
}