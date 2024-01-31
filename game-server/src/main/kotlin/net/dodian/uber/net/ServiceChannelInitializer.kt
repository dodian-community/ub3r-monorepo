package net.dodian.uber.net

import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.timeout.IdleStateHandler
import net.dodian.uber.game.TIMEOUT_SECONDS
import net.dodian.uber.net.codec.handshake.HandshakeDecoder
import java.util.concurrent.TimeUnit

class ServiceChannelInitializer(
    private val handler: ChannelInboundHandlerAdapter
) : ChannelInitializer<SocketChannel>() {

    override fun initChannel(ch: SocketChannel): Unit = with(ch.pipeline()) {
        addLast("handshakeDecoder", HandshakeDecoder())
        addLast("timeout", IdleStateHandler(true, TIMEOUT_SECONDS, TIMEOUT_SECONDS, TIMEOUT_SECONDS, TimeUnit.SECONDS))
        addLast("handler", handler)
    }
}