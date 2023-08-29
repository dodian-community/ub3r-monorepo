package net.dodian.uber.game.session

import io.netty.channel.Channel
import net.dodian.uber.net.codec.login.LoginRequest

class LoginSession(channel: Channel) : Session(channel) {
    private lateinit var request: LoginRequest

    override fun destroy() {}

    override fun messageReceived(message: Any) {
        if (message is LoginRequest) {
            handleLoginRequest(message)
        }
    }

    private fun handleLoginRequest(request: LoginRequest) {
        this.request = request

    }
}