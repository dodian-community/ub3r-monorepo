package net.dodian.uber.game.session

import io.netty.channel.Channel

abstract class Session(
    val channel: Channel
) {
    abstract fun destroy()
    abstract fun messageReceived(message: Any)
    protected fun channel() = channel
}