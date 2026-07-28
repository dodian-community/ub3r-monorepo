package net.dodian.uber.game.netty.protocol

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.EncoderException
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.netty.codec.ByteMessageEncoder
import net.dodian.uber.game.netty.codec.MessageType
import net.dodian.uber.game.netty.game.GamePacket
import net.dodian.uber.game.netty.game.GamePacketDecoder
import net.dodian.uber.game.netty.game.encode.TarnishOutboundPackets
import net.dodian.utilities.ISAACCipher
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TarnishProtocolCodecTest {
    @Test
    fun `decodes fixed and variable packets with ISAAC applied only to opcodes`() {
        val seed = intArrayOf(1, 2, 3, 4)
        val writerCipher = ISAACCipher(seed.copyOf())
        val channel = EmbeddedChannel(GamePacketDecoder())
        channel.attr(GamePacketDecoder.IN_CIPHER_KEY).set(ISAACCipher(seed.copyOf()))

        val bytes = Unpooled.buffer()
        bytes.writeByte((3 + writerCipher.getNextKey()) and 0xff)
        bytes.writeByte(1)
        bytes.writeByte((4 + writerCipher.getNextKey()) and 0xff)
        bytes.writeByte(3)
        bytes.writeBytes(byteArrayOf(10, 20, 30))
        channel.writeInbound(bytes)

        assertPacket(channel.readInbound(), 3, byteArrayOf(1))
        assertPacket(channel.readInbound(), 4, byteArrayOf(10, 20, 30))
        assertFalse(channel.finish())
    }

    @Test
    fun `decoder preserves state when a packet arrives one byte at a time`() {
        val seed = intArrayOf(5, 6, 7, 8)
        val writerCipher = ISAACCipher(seed.copyOf())
        val channel = EmbeddedChannel(GamePacketDecoder())
        channel.attr(GamePacketDecoder.IN_CIPHER_KEY).set(ISAACCipher(seed.copyOf()))
        val wire = byteArrayOf(((4 + writerCipher.getNextKey()) and 0xff).toByte(), 2, 55, 66)

        wire.forEach { channel.writeInbound(Unpooled.wrappedBuffer(byteArrayOf(it))) }

        assertPacket(channel.readInbound(), 4, byteArrayOf(55, 66))
        assertFalse(channel.finish())
    }

    @Test
    fun `password input uses a variable short encrypted payload`() {
        val seed = intArrayOf(7, 8, 9, 10)
        val writerCipher = ISAACCipher(seed.copyOf())
        val channel = EmbeddedChannel(GamePacketDecoder())
        channel.attr(GamePacketDecoder.IN_CIPHER_KEY).set(ISAACCipher(seed.copyOf()))
        val payload = byteArrayOf(1, 2, 3, 4)
        val wire = Unpooled.buffer()
        wire.writeByte((62 + writerCipher.getNextKey()) and 0xff)
        wire.writeShort(payload.size)
        wire.writeBytes(payload)

        channel.writeInbound(wire)

        assertPacket(channel.readInbound(), 62, payload)
        assertFalse(channel.finish())
    }

    @Test
    fun `encoder emits Tarnish fixed variable byte and variable short headers`() {
        assertArrayEquals(byteArrayOf(35, 1, 2, 3, 4), encode(ByteMessage.message(35).putInt(0x01020304)))
        assertArrayEquals(byteArrayOf(253.toByte(), 2, 65, 10), encode(ByteMessage.message(253, MessageType.VAR).putString("A")))
        assertArrayEquals(byteArrayOf(27, 0, 2, 7, 8), encode(ByteMessage.message(27, MessageType.VAR_SHORT).put(7).put(8)))
        assertArrayEquals(
            byteArrayOf(28, 0, 7, 1, 97, 98, 10, 112, 119, 10),
            encode(TarnishOutboundPackets.SendPasswordPrompt(1, "ab", "pw").encode()),
        )
    }

    @Test
    fun `encoder applies ISAAC to opcode only`() {
        val seed = intArrayOf(9, 10, 11, 12)
        val expectedCipher = ISAACCipher(seed.copyOf())
        val channel = EmbeddedChannel(ByteMessageEncoder(ISAACCipher(seed.copyOf())))
        channel.writeOutbound(ByteMessage.message(35).putInt(0x01020304))
        val header = channel.readOutbound<ByteBuf>()
        assertEquals((35 + expectedCipher.getNextKey()) and 0xff, header.readUnsignedByte().toInt())
        header.release()
        val payload = channel.readOutbound<ByteBuf>()
        val actual = ByteArray(payload.readableBytes())
        payload.readBytes(actual)
        payload.release()
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), actual)
        channel.finishAndReleaseAll()
    }

    @Test
    fun `encoder rejects wrong fixed payload size`() {
        val channel = EmbeddedChannel(ByteMessageEncoder())
        assertThrows(EncoderException::class.java) {
            channel.writeOutbound(ByteMessage.message(97).putShort(123))
        }
        channel.finishAndReleaseAll()
    }

    private fun encode(message: ByteMessage): ByteArray {
        val channel = EmbeddedChannel(ByteMessageEncoder())
        channel.writeOutbound(message)
        val output = ArrayList<Byte>()
        while (true) {
            val buffer = channel.readOutbound<ByteBuf>() ?: break
            while (buffer.isReadable) output += buffer.readByte()
            buffer.release()
        }
        channel.finishAndReleaseAll()
        return output.toByteArray()
    }

    private fun assertPacket(packet: GamePacket, opcode: Int, payload: ByteArray) {
        assertEquals(opcode, packet.opcode())
        assertEquals(payload.size, packet.size())
        val actual = ByteArray(packet.payload().readableBytes())
        packet.payload().readBytes(actual)
        packet.payload().release()
        assertArrayEquals(payload, actual)
    }
}
