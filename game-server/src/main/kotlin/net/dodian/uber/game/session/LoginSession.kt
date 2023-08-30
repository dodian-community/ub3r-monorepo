package net.dodian.uber.game.session

import com.github.michaelbull.logging.InlineLogger
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import net.dodian.context
import net.dodian.services.impl.GameService
import net.dodian.services.impl.LoginService
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.release.GameRelease
import net.dodian.uber.net.codec.game.GameMessageDecoder
import net.dodian.uber.net.codec.game.GameMessageEncoder
import net.dodian.uber.net.codec.login.LoginRequest
import net.dodian.uber.net.codec.login.LoginResponse
import net.dodian.uber.net.codec.login.STATUS_LOGIN_SERVER_REJECTED_SESSION
import net.dodian.uber.net.codec.login.STATUS_OK
import net.dodian.uber.net.codec.game.GamePacketDecoder
import net.dodian.uber.net.codec.game.GamePacketEncoder

private val logger = InlineLogger()

class LoginSession(channel: Channel) : Session(channel) {
    private lateinit var request: LoginRequest

    override fun destroy() {}

    fun handlePlayerLoaderResponse(request: LoginRequest, response: PlayerLoaderResponse) {
        this.request = request

        if (response.status != STATUS_OK) {
            sendLoginFailure(response.status)
            return
        }

        val service = context.service<GameService>()
        val player = response.player

        if (player != null) {
            service.registerPlayer(player, this)
        } else {
            logger.debug { "Couldn't login player, because the response didn't have a player..." }
            sendLoginFailure(STATUS_LOGIN_SERVER_REJECTED_SESSION)
        }
    }

    fun sendLoginFailure(status: Int) {
        val response = LoginResponse(status, 0, false)
        channel.writeAndFlush(response).addListeners(ChannelFutureListener.CLOSE)
    }

    fun sendLoginSuccess(player: Player) {
        val randomPair = request.randomPair

        val session = GameSession(channel, player, request.reconnecting)
        channel.attr(SESSION_KEY).set(session)
        player.session = session

        val rights = 3
        channel.writeAndFlush(LoginResponse(STATUS_OK, rights, false))

        val release = GameRelease()

        channel.pipeline().addFirst("messageEncoder", GameMessageEncoder(release))
        channel.pipeline().addBefore("messageEncoder", "gameEncoder", GamePacketEncoder(randomPair.encodingRandom))

        channel.pipeline().addBefore("handler", "gameDecoder", GamePacketDecoder(randomPair.decodingRandom, release))
        channel.pipeline().addAfter("gameDecoder", "messageDecoder", GameMessageDecoder(release))

        channel.pipeline().remove("loginDecoder")
        channel.pipeline().remove("loginEncoder")
    }

    override fun messageReceived(message: Any) {
        if (message is LoginRequest) {
            handleLoginRequest(message)
        }
    }

    private fun handleLoginRequest(request: LoginRequest) {
        val service = context.service<LoginService>()
        service.submitLoadRequest(this, request)
    }
}