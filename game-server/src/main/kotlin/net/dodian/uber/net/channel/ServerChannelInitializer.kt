package net.dodian.uber.net.channel

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import net.dodian.uber.net.protocol.Protocol
import net.dodian.uber.net.protocol.ProtocolDecoder
import net.dodian.uber.net.protocol.ProtocolEncoder

class ServerChannelInitializer(
    private val serverChannelHandler: ServerChannelHandler,
    private val serviceUpstream: Protocol,
    private val serviceDownstream: Protocol
) : ChannelInitializer<SocketChannel>() {

    override fun initChannel(channel: SocketChannel) {
        channel.pipeline().addLast(
            ProtocolDecoder(serviceUpstream),
            ProtocolEncoder(serviceDownstream),
            serverChannelHandler
        )
    }
}