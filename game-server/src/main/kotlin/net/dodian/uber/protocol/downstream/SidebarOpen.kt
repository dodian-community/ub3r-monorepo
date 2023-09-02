package net.dodian.uber.protocol.downstream

import net.dodian.uber.protocol.packet.DownstreamPacket

data class SidebarOpen(
    val sidebarId: Int
) : DownstreamPacket