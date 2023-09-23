package net.dodian.uber.net

import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpRequestEncoder
import io.netty.handler.timeout.IdleStateHandler

private const val MAX_REQUEST_LENGTH = 8192

class HttpChannelInitializer(
    private val handler: ChannelInboundHandlerAdapter
) : ChannelInitializer<SocketChannel>() {

    override fun initChannel(ch: SocketChannel): Unit = with(ch.pipeline()) {
        addLast("decoder", HttpRequestDecoder())
        addLast("chunker", HttpObjectAggregator(MAX_REQUEST_LENGTH))

        addLast("encoder", HttpRequestEncoder())

        addLast("timeout", IdleStateHandler(IDLE_TIME, 0, 0))
        addLast("handler", handler)
    }
}