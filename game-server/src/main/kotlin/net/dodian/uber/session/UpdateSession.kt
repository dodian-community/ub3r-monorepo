package net.dodian.uber.session

import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpRequest
import net.dodian.context
import net.dodian.uber.net.codec.jaggrab.JagGrabRequest
import net.dodian.uber.net.codec.update.OnDemandRequest
import net.dodian.uber.services.UpdateService

class UpdateSession(
    channel: Channel
) : Session(channel) {

    override fun destroy() {
        channel.close()
    }

    override fun messageReceived(message: Any) {
        val dispatcher = context.service<UpdateService>().dispatcher

        when (message) {
            is OnDemandRequest -> dispatcher.dispatch(channel, message)
            is JagGrabRequest -> dispatcher.dispatch(channel, message)
            is HttpRequest -> dispatcher.dispatch(channel, message)
            else -> error("Unknown message type. (type=${message::class.simpleName})")
        }
    }
}