package net.dodian.uber.services.impl

import com.github.michaelbull.logging.InlineLogger
import com.google.common.util.concurrent.AbstractIdleService
import kotlinx.coroutines.*
import net.dodian.uber.context
import net.dodian.uber.services.Service
import net.dodian.uber.game.GamePulseHandler
import net.dodian.uber.game.PULSE_DELAY
import net.dodian.uber.game.dispatcher.main.MainCoroutineScope
import net.dodian.uber.game.extensions.sendInitialMessages
import net.dodian.uber.game.job.GameBootTaskScheduler
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.message.handlers.MessageHandlerChainSet
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.process.GameProcess
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
import kotlin.system.measureNanoTime

private const val DE_REGISTRATIONS_PER_CYCLE = 50
private const val REGISTRATIONS_PER_CYCLE = 25
private const val GAME_TICK_DELAY = 600

private val logger = InlineLogger()

class GameService(
    private val process: GameProcess,
    private val bootTasks: GameBootTaskScheduler,
    private val coroutineScope: MainCoroutineScope,
    private val handlers: MessageHandlerChainSet = MessageHandlerChainSet()
) : AbstractIdleService() {

    private var excessCycleNanos = 0L

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

    fun start() {
        executor.scheduleAtFixedRate(GamePulseHandler(this), PULSE_DELAY, PULSE_DELAY, TimeUnit.MILLISECONDS)
    }

    fun registerPlayer(player: Player, session: LoginSession) {
        newPlayers.add(LoginPlayerRequest(player, session))
    }

    fun unregisterPlayer(player: Player) {
        oldPlayers.add(player)
    }

    private fun CoroutineScope.start(delay: Int) = launch {
        while (isActive) {
            val elapsedNanos = measureNanoTime { process.cycle() } + excessCycleNanos
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos)
            val overdue = elapsedMillis > delay
            val sleepTime = if (overdue) {
                val elapsedCycleCount = elapsedMillis / delay
                val upcomingCycleDelay = (elapsedCycleCount + 1) * delay
                upcomingCycleDelay - elapsedMillis
            } else {
                delay - elapsedMillis
            }
            if (overdue) logger.error { "Cycle took too long (elapsed=${elapsedMillis}ms, sleep=${sleepTime}ms)" }
            excessCycleNanos = elapsedNanos - TimeUnit.MILLISECONDS.toNanos(elapsedMillis)
            delay(sleepTime)
        }
    }

    private fun GameBootTaskScheduler.execute(): Unit = runBlocking {
        executeNonBlocking()
        executeBlocking(this)
    }

    override fun startUp() {
        bootTasks.execute()
        process.startUp()
        coroutineScope.start(GAME_TICK_DELAY)
    }

    override fun shutDown() {
        if (isRunning) {
            coroutineScope.cancel()
            process.shutDown()
        }
    }
}