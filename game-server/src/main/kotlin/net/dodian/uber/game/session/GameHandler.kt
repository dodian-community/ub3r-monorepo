package net.dodian.uber.game.session

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.AttributeKey

val SESSION_KEY: AttributeKey<Session> = AttributeKey.valueOf("session")

@Sharable
class GameHandler : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {

    }
}