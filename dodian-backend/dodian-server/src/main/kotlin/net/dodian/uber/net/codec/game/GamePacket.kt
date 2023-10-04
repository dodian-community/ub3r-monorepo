package net.dodian.uber.net.codec.game

import io.netty.buffer.ByteBuf
import net.dodian.uber.net.message.meta.PacketType

data class GamePacket(
    val opcode: Int,
    val type: PacketType,
    val payload: ByteBuf,
    val length: Int = payload.readableBytes()
)