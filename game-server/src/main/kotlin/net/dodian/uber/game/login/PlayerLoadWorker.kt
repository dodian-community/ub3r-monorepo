package net.dodian.uber.game.login

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.io.player.PlayerSerializer
import net.dodian.uber.game.session.LoginSession
import net.dodian.uber.game.session.PlayerLoaderResponse
import net.dodian.uber.net.codec.login.LoginRequest
import net.dodian.uber.net.codec.login.STATUS_COULD_NOT_COMPLETE

private val logger = InlineLogger()

class PlayerLoadWorker(
    private val loader: PlayerSerializer,
    private val session: LoginSession,
    private val request: LoginRequest
) : Runnable {

    override fun run() {
        try {
            session.handlePlayerLoaderResponse(request, loader.loadPlayer(request.credentials))
        } catch (cause: Exception) {
            logger.error(cause) { "Unable to load player's game..." }
            session.handlePlayerLoaderResponse(request, PlayerLoaderResponse(STATUS_COULD_NOT_COMPLETE))
        }
    }
}