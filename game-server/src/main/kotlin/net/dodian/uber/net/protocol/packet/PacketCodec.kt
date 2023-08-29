package net.dodian.uber.net.protocol.packet

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.openrs2.crypto.StreamCipher

abstract class PacketCodec<T : Packet>(
    val type: Class<T>,
    val opcode: Int
) {
    abstract fun decode(buf: ByteBuf, cipher: StreamCipher): T
    abstract fun encode(packet: T, buf: ByteBuf, cipher: StreamCipher)
    abstract fun isLengthReadable(buf: ByteBuf): Boolean
    abstract fun readLength(buf: ByteBuf): Int
    abstract fun offsetLength(buf: ByteBuf)
    abstract fun setLength(buf: ByteBuf, offsetLengthWriterIndex: Int, length: Int)
    open fun allocateEncodeBuffer(allocateBuf: ByteBufAllocator, packet: T, preferDirect: Boolean): ByteBuf =
        if (preferDirect) allocateBuf.ioBuffer() else allocateBuf.heapBuffer()
}