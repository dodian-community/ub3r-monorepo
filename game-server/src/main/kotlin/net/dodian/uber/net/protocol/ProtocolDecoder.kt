package net.dodian.uber.net.protocol

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import net.dodian.uber.net.protocol.packet.PacketCodec
import net.dodian.uber.net.protocol.packet.UpstreamDiscardPacket
import org.openrs2.crypto.StreamCipher
import org.openrs2.crypto.NopStreamCipher

private sealed class Stage {
    object ReadOpcode : Stage()
    object ReadLength : Stage()
    object ReadPayload : Stage()
}

class ProtocolDecoder(
    var protocol: Protocol,
    var cipher: StreamCipher = NopStreamCipher
) : ByteToMessageDecoder() {
    private var stage: Stage = Stage.ReadOpcode
    private var length = 0

    private lateinit var decoder: PacketCodec<*>

    init {
        isSingleDecode = true
    }

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        when (stage) {
            Stage.ReadOpcode -> {
                if (!input.isReadable)
                    return

                val opcode = (input.readUnsignedByte().toInt() - cipher.nextInt()) and 0xFF
                decoder = protocol.decoder(opcode) ?: error("Decoder not found for opcode (opcode=$opcode).")
                stage = Stage.ReadLength
            }

            Stage.ReadLength -> {
                if (!decoder.isLengthReadable(input))
                    return

                length = decoder.readLength(input)
                stage = Stage.ReadPayload
            }

            Stage.ReadPayload -> {
                if (input.readableBytes() < length)
                    return

                val payload = input.readSlice(length)
                val packet = decoder.decode(payload, cipher)
                check(!payload.isReadable) {
                    "Decoder (${decoder.javaClass.simpleName}) did not fully read payload. " +
                            "(read ${payload.readerIndex()} bytes, left with ${payload.readableBytes()})"
                }

                if (packet !is UpstreamDiscardPacket)
                    out += packet

                stage = Stage.ReadOpcode
            }
        }
    }
}