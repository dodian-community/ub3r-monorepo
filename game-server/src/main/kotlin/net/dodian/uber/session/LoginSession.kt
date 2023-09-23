package net.dodian.uber.session

import com.github.michaelbull.logging.InlineLogger
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import net.dodian.context
import net.dodian.uber.game.modelkt.entity.Player
import net.dodian.uber.io.player.PlayerLoaderResponse
import net.dodian.uber.net.codec.game.GameMessageDecoder
import net.dodian.uber.net.codec.game.GameMessageEncoder
import net.dodian.uber.net.codec.game.GamePacketDecoder
import net.dodian.uber.net.codec.game.GamePacketEncoder
import net.dodian.uber.net.codec.login.LoginRequest
import net.dodian.uber.net.codec.login.LoginResponse
import net.dodian.uber.net.codec.login.STATUS_OK
import net.dodian.uber.services.GameService
import net.dodian.uber.services.LoginService

private val logger = InlineLogger()

class LoginSession(channel: Channel) : Session(channel) {
    private lateinit var request: LoginRequest

    override fun destroy() {
    }

    override fun messageReceived(message: Any) {
        if (message is LoginRequest)
            handleLoginRequest(message)
    }

    private fun handleLoginRequest(request: LoginRequest) {
        logger.debug { "Handling login request..." }
        val service = context.service<LoginService>()
        service.submitLoadRequest(this, request)
    }

    fun handlePlayerLoaderResponse(request: LoginRequest, response: PlayerLoaderResponse) {
        this.request = request

        val service = context.service<GameService>()
        val player = response.player
            ?: return sendLoginFailure(response.status)

        service.registerPlayer(player, this)
    }

    fun sendLoginFailure(status: Int) {
        val response = LoginResponse(status, 0, false)
        channel.writeAndFlush(response).addListeners(ChannelFutureListener.CLOSE)
    }

    fun sendLoginSuccess(player: Player) {
        val randomPair = request.randomPair
        val flagged = false

        val session = GameSession(channel, player, request.isReconnecting)
        channel.attr(SESSION_KEY).set(session)
        player.session = session

        val rights = player.playerRights
        channel.writeAndFlush(LoginResponse(STATUS_OK, rights, flagged))

        val pipeline = channel.pipeline()
        pipeline.addFirst("messageEncoder", GameMessageEncoder(context.protocol.encoders))
        pipeline.addBefore("messageEncoder", "gameEncoder", GamePacketEncoder(randomPair.encodingRandom))

        pipeline.addBefore("handler", "gameDecoder", GamePacketDecoder(randomPair.decodingRandom, context.protocol))
        pipeline.addAfter("gameDecoder", "messageDecoder", GameMessageDecoder(context.protocol.decoders))

        pipeline.remove("loginDecoder")
        pipeline.remove("loginEncoder")
    }
}