package net.dodian.uber.game.session

import io.netty.channel.Channel

class UpdateSession(
    channel: Channel
) : Session(channel) {

    override fun destroy() {
        channel.close()
    }

    override fun messageReceived(message: Any) {
        error("Unknown message type. (type=${message::class.simpleName})")
    }
}