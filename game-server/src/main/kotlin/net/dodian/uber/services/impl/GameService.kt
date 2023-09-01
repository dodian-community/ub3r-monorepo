package net.dodian.uber.services.impl

import net.dodian.uber.context
import net.dodian.uber.services.Service
import net.dodian.uber.game.GamePulseHandler
import net.dodian.uber.game.PULSE_DELAY
import net.dodian.uber.game.extensions.sendInitialMessages
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.message.handlers.MessageHandlerChainSet
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.session.LoginSession
import net.dodian.uber.game.session.PlayerManager
import net.dodian.uber.game.sync.ClientSynchronizer
import net.dodian.uber.game.sync.ParallelClientSynchronizer
import net.dodian.uber.net.codec.login.STATUS_GAME_UPDATED
import net.dodian.uber.net.codec.login.STATUS_SERVER_FULL
import net.dodian.utilities.ThreadUtil
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private const val DE_REGISTRATIONS_PER_CYCLE = 50
private const val REGISTRATIONS_PER_CYCLE = 25

class GameService(
    private val handlers: MessageHandlerChainSet = MessageHandlerChainSet()
) : Service() {

    data class LoginPlayerRequest(
        val player: Player,
        val session: LoginSession
    )

    private val synchronizer: ClientSynchronizer = ParallelClientSynchronizer()
    private val executor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(ThreadUtil.create("GameService"))

    private val newPlayers: Queue<LoginPlayerRequest> = ConcurrentLinkedQueue()
    private val oldPlayers: Queue<Player> = ConcurrentLinkedQueue()

    private fun init() {

    }

    @Synchronized
    fun pulse() {
        finalizeRegistrations()
        finalizeDeregistrations()

        val playerManager = context.handler<PlayerManager>()
        val players = playerManager.players

        for (player in players) {
            if (player.session == null)
                continue

            player.session.handlePendingMessages(handlers)
        }
    }

    private fun finalizeRegistrations() {
        for (i in 0 until REGISTRATIONS_PER_CYCLE) {
            val request = newPlayers.poll() ?: continue

            val player = request.player
            val playerManager = context.handler<PlayerManager>()

            if (playerManager.isPlayerOnline(player)) {
                request.session.sendLoginFailure(STATUS_GAME_UPDATED)
            } else if (playerManager.isFull) {
                request.session.sendLoginFailure(STATUS_SERVER_FULL)
            } else {
                request.session.sendLoginSuccess(player)
                finalizePlayerRegistration(player)
            }
        }
    }

    private fun finalizeDeregistrations() {
        val loginService = context.service<LoginService>()

        for (i in 0 until DE_REGISTRATIONS_PER_CYCLE) {
            val player = oldPlayers.poll() ?: break

            loginService.submitSaveRequest(player.session, player)
        }
    }

    @Synchronized
    fun finalizePlayerRegistration(player: Player) {
        val playerManager = context.handler<PlayerManager>()
        playerManager.register(player)

        if (!player.session.isReconnecting)
            (player as Client).sendInitialMessages()
    }

    override fun start() {
        executor.scheduleAtFixedRate(GamePulseHandler(this), PULSE_DELAY, PULSE_DELAY, TimeUnit.MILLISECONDS)
    }

    fun registerPlayer(player: Player, session: LoginSession) {
        newPlayers.add(LoginPlayerRequest(player, session))
    }

    fun unregisterPlayer(player: Player) {
        oldPlayers.add(player)
    }
}