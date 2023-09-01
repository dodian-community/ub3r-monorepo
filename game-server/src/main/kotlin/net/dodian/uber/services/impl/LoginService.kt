package net.dodian.uber.services.impl

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.services.Service
import net.dodian.uber.game.io.player.DummyPlayerSerializer
import net.dodian.uber.game.io.player.PlayerSerializer
import net.dodian.uber.game.login.PlayerLoadWorker
import net.dodian.uber.game.login.PlayerSaveWorker
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.session.GameSession
import net.dodian.uber.game.session.LoginSession
import net.dodian.uber.game.session.PlayerLoaderResponse
import net.dodian.uber.net.codec.login.LoginRequest
import net.dodian.uber.net.codec.login.STATUS_GAME_UPDATED
import net.dodian.uber.net.codec.login.STATUS_OK
import net.dodian.utilities.ThreadUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val logger = InlineLogger()

class LoginService(
    private val serializer: PlayerSerializer = DummyPlayerSerializer()
) : Service() {

    private val executor: ExecutorService = Executors.newCachedThreadPool(ThreadUtil.create("LoginService"))

    fun submitLoadRequest(session: LoginSession, request: LoginRequest) {
        var response = STATUS_OK

        if (requiresUpdate(request)) {
            logger.debug { "Client requires an update (clientVersion=${request.releaseNumber})" }
            response = STATUS_GAME_UPDATED
        }

        if (response == STATUS_OK) {
            logger.info { "Yep, we're supposed to load the player now..." }
            executor.submit(PlayerLoadWorker(serializer, session, request))
        } else {
            logger.warn { "Player couldn't be loaded, status=$response" }
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

    override fun start() {}
}