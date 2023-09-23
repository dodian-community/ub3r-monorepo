package net.dodian.uber.net.codec.game

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import net.dodian.uber.net.message.GameProtocol
import net.dodian.uber.net.message.meta.PacketType
import net.dodian.uber.utils.StatefulFrameDecoder
import net.dodian.utilities.security.IsaacRandom

private val logger = InlineLogger()

class GamePacketDecoder(
    private val random: IsaacRandom,
    private val protocol: GameProtocol
) : StatefulFrameDecoder<GameDecoderState>(GameDecoderState.GAME_OPCODE) {
    var length: Int = 0
    var opcode: Int = -1
    var type: PacketType = PacketType.FIXED

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>, state: GameDecoderState) {
        when (state) {
            GameDecoderState.GAME_OPCODE -> decodeOpcode(input, out)
            GameDecoderState.GAME_LENGTH -> decodeLength(input)
            GameDecoderState.GAME_PAYLOAD -> decodePayload(input, out)
            else -> error("Illegal decoder state: $state")
        }
    }

    private fun decodeOpcode(buffer: ByteBuf, out: MutableList<Any>) {
        if (!buffer.isReadable) return

        val encryptedOpcode = buffer.readUnsignedByte().toInt()
        opcode = encryptedOpcode - random.nextInt() and 0xFF

        val metadata = protocol.metadata[opcode]
            ?: error("No metadata for opcode: $opcode")

        type = metadata.type
        when (type) {
            PacketType.FIXED -> {
                length = metadata.length
                if (length == 0) {
                    state = GameDecoderState.GAME_OPCODE
                    out.add(GamePacket(opcode, type, Unpooled.EMPTY_BUFFER))
                } else state = GameDecoderState.GAME_PAYLOAD
            }
            PacketType.VARIABLE_BYTE -> state = GameDecoderState.GAME_LENGTH
            else -> error("Illegal packet type: $type")
        }
    }

    private fun decodeLength(buffer: ByteBuf) {
        if (!buffer.isReadable) return

        length = buffer.readUnsignedByte().toInt()
        if (length != 0) state = GameDecoderState.GAME_PAYLOAD
    }

    private fun decodePayload(buffer: ByteBuf, out: MutableList<Any>) {
        if (buffer.readableBytes() < length) return

        val payload = buffer.readBytes(length)
        state = GameDecoderState.GAME_OPCODE
        out.add(GamePacket(opcode, type, payload))
    }
}