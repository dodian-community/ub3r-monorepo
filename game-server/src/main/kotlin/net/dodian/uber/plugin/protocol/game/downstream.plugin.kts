package net.dodian.uber.plugin.protocol.game

import net.dodian.uber.plugin.context
import net.dodian.uber.protocol.downstream.*

val packets = context().packetMap.downstream

packets.register<SidebarOpen> {
    opcode = 106
    encode { packet, buf ->
        // TODO: Not sure if this is the right buffer writer
        buf.writeByte(packet.sidebarId)
    }
}

packets.register<RegionUpdate> {
    opcode = 85
    encode { packet, buf ->
        // TODO: Not sure if this is the right buffer writer
        buf.writeByte(packet.region.getLocalY(packet.player))
        buf.writeByte(packet.region.getLocalX(packet.player))
    }
}

packets.register<Logout> {
    opcode = 109
    encode { _, _ -> }
}

packets.register<InterfaceClose> {
    opcode = 219
    encode { _, _ -> }
}

packets.register<RegionClear> {
    opcode = 64
    encode { packet, buf ->
        // TODO: Not sure if this is the right buffer writer
        buf.writeByte(packet.region.getLocalX(packet.player))
        buf.writeByte(packet.region.getLocalY(packet.player))
    }
}