package net.dodian.uber.net.codec.update

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

class UpdateEncoder : MessageToMessageEncoder<OnDemandResponse>() {

    override fun encode(ctx: ChannelHandlerContext, response: OnDemandResponse, out: MutableList<Any>) {
        val descriptor = response.descriptor
        val fileSize = response.fileSize
        val chunkId = response.chunkId
        val chunkData = response.chunkData

        val buffer = ctx.alloc().buffer(2 * Byte.SIZE_BYTES + 2 * Short.SIZE_BYTES + chunkData.readableBytes())
        buffer.writeByte(descriptor.type - 1)
        buffer.writeShort(descriptor.file)
        buffer.writeShort(fileSize)
        buffer.writeByte(chunkId)
        buffer.writeBytes(chunkData)

        out.add(buffer)
    }
}