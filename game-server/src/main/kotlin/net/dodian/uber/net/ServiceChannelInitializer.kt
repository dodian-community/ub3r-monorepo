package net.dodian.uber.net

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.timeout.IdleStateHandler
import net.dodian.uber.game.session.GameHandler
import net.dodian.uber.net.codec.handshake.HandshakeDecoder
import java.util.concurrent.TimeUnit

class ServiceChannelInitializer(
    private val handler: GameHandler
) : ChannelInitializer<SocketChannel>() {

    override fun initChannel(channel: SocketChannel) {
        val pipeline = channel.pipeline()
        pipeline.addLast("handshakeDecoder", HandshakeDecoder())
        pipeline.addLast("timeout", IdleStateHandler(
            true, TIMEOUT_SECONDS, TIMEOUT_SECONDS, TIMEOUT_SECONDS, TimeUnit.SECONDS
        ))
        pipeline.addLast("handler", handler)
    }
}