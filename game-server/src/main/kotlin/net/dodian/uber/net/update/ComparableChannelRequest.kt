package net.dodian.uber.net.update

import io.netty.channel.Channel

class ComparableChannelRequest<T : Comparable<T>>(
    channel: Channel,
    request: T
) : ChannelRequest<T>(channel, request), Comparable<ComparableChannelRequest<T>> {

    override fun compareTo(other: ComparableChannelRequest<T>) = request.compareTo(other.request)
}