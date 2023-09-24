package net.dodian.uber.services

import com.github.michaelbull.logging.InlineLogger
import net.dodian.context
import net.dodian.uber.game.GamePulseHandler
import net.dodian.uber.game.PULSE_DELAY
import net.dodian.uber.net.protocol.handlers.MessageHandlerChainSet
import net.dodian.uber.game.modelkt.entity.player.Player
import net.dodian.uber.game.sync.ClientSynchronizer
import net.dodian.uber.game.sync.ParallelClientSynchronizer
import net.dodian.uber.net.codec.login.STATUS_ACCOUNT_ONLINE
import net.dodian.uber.net.codec.login.STATUS_SERVER_FULL
import net.dodian.uber.session.LoginSession
import net.dodian.utilities.ThreadUtil
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val logger = InlineLogger()

private const val REGISTRATIONS_PER_CYCLE = 25
private const val DEREGISTRATIONS_PER_CYCLE = 50

class GameService(
    private val handlers: MessageHandlerChainSet = MessageHandlerChainSet(),
    private val synchronizer: ClientSynchronizer = ParallelClientSynchronizer()
) : Service {

    private val executor = Executors.newSingleThreadScheduledExecutor(ThreadUtil.create("GameService"))

    override fun start() {
        executor.scheduleAtFixedRate(GamePulseHandler(this), PULSE_DELAY, PULSE_DELAY, TimeUnit.MILLISECONDS)
    }

    fun shutdown(natural: Boolean) {
        executor.shutdownNow()
    }

    data class LoginPlayerRequest(
        val player: Player,
        val session: LoginSession
    )

    private val newPlayers: Queue<LoginPlayerRequest> = ConcurrentLinkedQueue()
    private val oldPlayers: Queue<Player> = ConcurrentLinkedQueue()

    @Synchronized
    fun pulse() {
        val world = context.world

        finalizeRegistrations()
        finalizeUnregistrations()

        val players = world.playerRepository
        players.forEach {
            it.session?.handlePendingMessages(handlers)
        }

        world.pulse()
        synchronizer.synchronize(players, world.npcRepository)
    }

    fun registerPlayer(player: Player, session: LoginSession) {
        newPlayers.add(LoginPlayerRequest(player, session))
    }

    fun unregisterPlayer(player: Player) {
        oldPlayers.add(player)
    }

    private fun finalizeRegistrations() {
        val world = context.world

        repeat(REGISTRATIONS_PER_CYCLE) {
            val request = newPlayers.poll()
                ?: return@repeat

            val player = request.player

            if (world.isPlayerOnline(player.username)) {
                request.session.sendLoginFailure(STATUS_ACCOUNT_ONLINE)
            } else if (world.playerRepository.size >= 3) {
                request.session.sendLoginFailure(STATUS_SERVER_FULL)
            } else {
                request.session.sendLoginSuccess(player)
                finalizePlayerRegistration(player)
            }
        }
    }

    private fun finalizeUnregistrations() {
        val loginService = context.service<LoginService>()

        repeat(DEREGISTRATIONS_PER_CYCLE) {
            val player = oldPlayers.poll()
            if (player?.session == null)
                return@repeat

            loginService.submitSaveRequest(player.session!!, player)
        }
    }

    @Synchronized
    fun finalizePlayerRegistration(player: Player) {
        val world = context.world

        world.register(player)

        if (player.session?.reconnecting != true)
            player.sendInitialMessages()
    }

    @Synchronized
    fun finalizePlayerUnregistration(player: Player) {
        context.world.unregister(player)
    }
}