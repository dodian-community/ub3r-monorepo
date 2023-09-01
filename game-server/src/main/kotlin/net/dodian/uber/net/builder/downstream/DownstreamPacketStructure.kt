package net.dodian.uber.net.builder.downstream

import io.netty.buffer.ByteBuf
import net.dodian.uber.protocol.packet.DownstreamPacket

data class DownstreamPacketStructure<T : DownstreamPacket>(
    val opcode: Int,
    val length: Int,
    val encoder: (T, ByteBuf) -> Unit
)