package net.dodian.uber.net.update

import io.netty.channel.Channel

open class ChannelRequest<T>(
    val channel: Channel,
    val request: T
)