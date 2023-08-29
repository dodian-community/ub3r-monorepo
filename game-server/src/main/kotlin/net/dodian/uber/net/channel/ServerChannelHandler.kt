package net.dodian.uber.net.channel

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import net.dodian.uber.net.protocol.packet.UpstreamPacket

class ServerChannelHandler : SimpleChannelInboundHandler<UpstreamPacket>(UpstreamPacket::class.java) {

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: UpstreamPacket?) {

    }
}