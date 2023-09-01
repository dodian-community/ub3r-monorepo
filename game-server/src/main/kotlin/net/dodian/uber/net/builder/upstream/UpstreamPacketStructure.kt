package net.dodian.uber.net.builder.upstream

import io.netty.buffer.ByteBuf
import net.dodian.uber.protocol.packet.UpstreamPacket

data class UpstreamPacketStructure<T : UpstreamPacket>(
    val opcode: Int,
    val length: Int,
    val decoder: (ByteBuf) -> T
)