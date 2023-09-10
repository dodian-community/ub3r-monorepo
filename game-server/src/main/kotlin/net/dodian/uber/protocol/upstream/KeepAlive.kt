package net.dodian.uber.protocol.upstream

import net.dodian.uber.protocol.packet.UpstreamPacket

data class KeepAlive(
    val createdAt: Long = System.currentTimeMillis()
) : UpstreamPacket