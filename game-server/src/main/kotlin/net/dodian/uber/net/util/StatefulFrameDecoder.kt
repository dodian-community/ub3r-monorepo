package net.dodian.uber.net.util

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

abstract class StatefulFrameDecoder<T : Enum<T>>(
    protected var state: T
) : ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) =
        decode(ctx, input, out, state)

    protected abstract fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>, state: T)
}