package net.dodian.uber.net.codec.update

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import org.apollo.cache.FileDescriptor

class UpdateDecoder : ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf, out: MutableList<Any>) {
        if (buffer.readableBytes() < 4)
            return

        val type = buffer.readUnsignedByte() + 1
        val file = buffer.readUnsignedShort()

        val value = buffer.readUnsignedByte().toInt()
        val priority = Priority.byValue(value)
            ?: error("Priority out of range - received $value")

        val desc = FileDescriptor(type, file)
        out.add(OnDemandRequest(desc, priority))
    }
}