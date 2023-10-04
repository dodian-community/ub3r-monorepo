package net.dodian.uber.net.update

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import net.dodian.uber.net.codec.jaggrab.JagGrabRequest
import net.dodian.uber.net.codec.jaggrab.JagGrabResponse
import net.dodian.uber.net.update.resource.ResourceProvider
import net.dodian.uber.net.update.resource.VirtualResourceProvider
import org.apollo.cache.IndexedFileSystem

class JagGrabRequestWorker(
    dispatcher: UpdateDispatcher,
    val fs: IndexedFileSystem
) : RequestWorker<JagGrabRequest, ResourceProvider>(dispatcher, VirtualResourceProvider(fs)) {

    override fun nextRequest(dispatcher: UpdateDispatcher) = dispatcher.nextJagGrabRequest

    override fun service(provider: ResourceProvider, channel: Channel, request: JagGrabRequest): Boolean {
        val buffer = provider.get(request.filePath)

        if (buffer != null) {
            val wrapped = Unpooled.wrappedBuffer(buffer)
            channel.writeAndFlush(JagGrabResponse(wrapped))
                .addListener(ChannelFutureListener.CLOSE)
        } else channel.close()

        return true
    }
}