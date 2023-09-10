package net.dodian.uber.protocol.packet

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import net.dodian.utilities.security.IsaacRandom

abstract class PacketCodec<T : Packet>(
    val type: Class<T>,
    val opcode: Int
) {
    abstract fun decode(buf: ByteBuf, random: IsaacRandom): T

    abstract fun encode(packet: T, buf: ByteBuf, random: IsaacRandom)

    abstract fun isLengthReadable(buf: ByteBuf): Boolean

    abstract fun readLength(buf: ByteBuf): Int

    abstract fun offsetLength(buf: ByteBuf)

    abstract fun setLength(buf: ByteBuf, offsetLengthWriterIndex: Int, length: Int)

    open fun allocEncodeBuffer(alloc: ByteBufAllocator, packet: T, preferDirect: Boolean): ByteBuf {
        return if (preferDirect) {
            alloc.ioBuffer()
        } else {
            alloc.heapBuffer()
        }
    }
}