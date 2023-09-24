package net.dodian.uber.services

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.io.player.DummyPlayerSerializer
import net.dodian.uber.io.player.PlayerSerializer
import net.dodian.uber.login.PlayerLoadWorker
import net.dodian.uber.login.PlayerSaveWorker
import net.dodian.uber.net.codec.login.LoginRequest
import net.dodian.uber.net.codec.login.STATUS_GAME_UPDATED
import net.dodian.uber.net.codec.login.STATUS_OK
import net.dodian.uber.session.GameSession
import net.dodian.uber.session.LoginSession
import net.dodian.uber.io.player.PlayerLoaderResponse
import net.dodian.utilities.ThreadUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val logger = InlineLogger()

class LoginService(
    private val serializer: PlayerSerializer = DummyPlayerSerializer()
) : Service {

    override fun start() {

    }

    private val executor: ExecutorService = Executors.newCachedThreadPool(ThreadUtil.create("LoginService"))

    fun submitLoadRequest(session: LoginSession, request: LoginRequest) {
        var response = STATUS_OK

        if (requiresUpdate(request)) {
            logger.debug { "Client requires an update (clientVersion=${request.releaseNumber})" }
            response = STATUS_GAME_UPDATED
        }

        logger.debug { "Submitting load request, with response: $response" }
        if (response == STATUS_OK) {
            executor.submit(PlayerLoadWorker(serializer, session, request))
        } else {
            session.handlePlayerLoaderResponse(request, PlayerLoaderResponse(response))
        }
    }

    fun submitSaveRequest(session: GameSession, player: Player) {
        executor.submit(PlayerSaveWorker(serializer, session, player))
    }

    private fun requiresUpdate(request: LoginRequest): Boolean {
        val release = 317
        if (release != request.releaseNumber) return true

        // TODO: Implement crc check?
        val clientCrcs = request.archiveCrcs
        val serverCrcs = intArrayOf()

        return false
    }
}