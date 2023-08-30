package net.dodian.uber.net.codec.game

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import net.dodian.uber.net.meta.PacketType
import net.dodian.uber.net.release.Release
import net.dodian.uber.net.util.StatefulFrameDecoder
import net.dodian.utilities.security.IsaacRandom

class GamePacketDecoder(
    val random: IsaacRandom,
    val release: Release
) : StatefulFrameDecoder<GameDecoderState>(GameDecoderState.GAME_OPCODE) {
    private var length: Int = 0
    private var opcode: Int = -1
    private var type: PacketType? = null

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>, state: GameDecoderState) {
        when (state) {
            GameDecoderState.GAME_OPCODE -> decodeOpcode(input, out)
            GameDecoderState.GAME_LENGTH -> decodeLength(input)
            GameDecoderState.GAME_PAYLOAD -> decodePayload(input, out)
        }
    }

    private fun decodeLength(buffer: ByteBuf) {
        if (!buffer.isReadable)
            return

        length = buffer.readUnsignedByte().toInt()
        if (length != 0)
            state = GameDecoderState.GAME_PAYLOAD
    }

    private fun decodeOpcode(buffer: ByteBuf, out: MutableList<Any>) {
        if (!buffer.isReadable)
            return

        val encryptedOpcode = buffer.readUnsignedByte().toInt()
        opcode = encryptedOpcode - random.nextInt() and 0xFF

        val metaData = release.incomingPacketMetaData(opcode)
        type = metaData.type

        when (type) {
            PacketType.FIXED -> {
                length = metaData.length
                if (length == 0) {
                    state = GameDecoderState.GAME_OPCODE
                    out.add(GamePacket(opcode = opcode, PacketType.FIXED, payload = Unpooled.EMPTY_BUFFER))
                } else state = GameDecoderState.GAME_PAYLOAD
            }

            PacketType.VARIABLE_BYTE -> state = GameDecoderState.GAME_LENGTH

            else -> error("Illegal packet type: $type")
        }
    }

    private fun decodePayload(buffer: ByteBuf, out: MutableList<Any>) {
        if (buffer.readableBytes() < length)
            return

        val finalType = type ?: error("Type is not set...")
        val payload = buffer.readBytes(length)
        state = GameDecoderState.GAME_OPCODE
        out.add(GamePacket(opcode = opcode, type = finalType, payload = payload))
    }
}