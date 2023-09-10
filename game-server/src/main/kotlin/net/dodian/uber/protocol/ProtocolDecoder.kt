package net.dodian.uber.protocol

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import net.dodian.uber.protocol.packet.PacketCodec
import net.dodian.uber.protocol.packet.UpstreamPacket
import net.dodian.utilities.security.IsaacRandom

private sealed class Stage {
    object ReadOpcode : Stage()
    object ReadLength : Stage()
    object ReadPayload : Stage()
}

private val logger = InlineLogger()

class ProtocolDecoder(
    val protocol: Protocol,
    val random: IsaacRandom
) : ByteToMessageDecoder() {

    private var stage: Stage = Stage.ReadOpcode
    private var length = 0

    private lateinit var decoder: PacketCodec<*>

    init {
        isSingleDecode = true
    }

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        if (stage == Stage.ReadOpcode) {
            if (!input.isReadable) return
            val opcode = (input.readUnsignedByte().toInt() - random.nextInt()) and 0xFF
            logger.info {"Received opcode: $opcode"}
            decoder = protocol.getDecoder(opcode) ?: error("Decoder not found for opcode '$opcode'.")
            stage = Stage.ReadLength
        }

        if (stage == Stage.ReadLength) {
            if (!decoder.isLengthReadable(input)) return
            length = decoder.readLength(input)
            stage = Stage.ReadPayload
        }

        if (stage == Stage.ReadPayload) {
            if (input.readableBytes() < length) return
            val payload = input.readSlice(length)
            val packet = decoder.decode(payload, random)
            check(!payload.isReadable) {
                "Decoder (${decoder.javaClass.simpleName}) did not fully read payload. " +
                    "(read ${payload.readerIndex()} bytes, left with ${payload.readableBytes()})"
            }
            if (packet !is UpstreamPacket) {
                out += packet
            }
            stage = Stage.ReadOpcode
        }
    }
}