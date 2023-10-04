package net.dodian.uber.net.update

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import net.dodian.uber.net.codec.update.OnDemandRequest
import net.dodian.uber.net.codec.update.OnDemandResponse
import org.apollo.cache.IndexedFileSystem
import kotlin.math.min

private const val CHUNK_LENGTH = 500

class OnDemandRequestWorker(
    dispatcher: UpdateDispatcher,
    private val fs: IndexedFileSystem
) : RequestWorker<OnDemandRequest, IndexedFileSystem>(dispatcher, fs) {

    override fun nextRequest(dispatcher: UpdateDispatcher) = dispatcher.nextOnDemandRequest

    override fun service(provider: IndexedFileSystem, channel: Channel, request: OnDemandRequest): Boolean {
        val descriptor = request.descriptor
        val buffer = Unpooled.wrappedBuffer(fs.getFile(descriptor))
        val length = buffer.readableBytes()

        for (chunk in 0 until buffer.readableBytes()) {
            val chunkSize = min(buffer.readableBytes(), CHUNK_LENGTH)
            val response = OnDemandResponse(descriptor, length, chunk, buffer.readBytes(chunkSize))
            channel.writeAndFlush(response)
        }

        return true
    }
}