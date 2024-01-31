package net.dodian.uber.net.session

import com.github.michaelbull.logging.InlineLogger
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.AttributeKey


private val logger = InlineLogger()

val SESSION_KEY: AttributeKey<Session> = AttributeKey.valueOf("session")

@Sharable
class Ub3rHandler : ChannelInboundHandlerAdapter() {
}