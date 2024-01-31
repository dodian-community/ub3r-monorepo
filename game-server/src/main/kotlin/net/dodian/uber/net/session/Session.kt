package net.dodian.uber.net.session

import io.netty.channel.Channel

abstract class Session(
    val channel: Channel
) {
    abstract fun destroy()
    abstract fun messageReceived(message: Any)
    protected fun channel() = channel
}