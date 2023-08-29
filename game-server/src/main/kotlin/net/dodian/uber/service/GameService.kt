package net.dodian.uber.service

import net.dodian.uber.game.GamePulseHandler
import net.dodian.uber.game.PULSE_DELAY
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.session.LoginSession
import net.dodian.uber.game.sync.ClientSynchronizer
import net.dodian.uber.game.sync.ParallelClientSynchronizer
import net.dodian.utilities.ThreadUtil
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private const val DE_REGISTRATIONS_PER_CYCLE = 50
private const val REGISTRATIONS_PER_CYCLE = 25

class GameService : Service() {

    sealed class LoginPlayerRequest(
        val player: Player,
        val session: LoginSession
    )

    private val synchronizer: ClientSynchronizer = ParallelClientSynchronizer()
    private val executor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(ThreadUtil.create("GameService"))

    private val newPlayers: Queue<Player> = ConcurrentLinkedQueue()
    private val oldPlayers: Queue<Player> = ConcurrentLinkedQueue()

    @Synchronized
    fun pulse() {
        //finalizeRegistrations()
        //finalizeUnregistrations()
        //val players: Iterable<Player> = world.getPlayerRepository()
        //for (player in players) {
        //    val session: GameSession = player.getSession()
        //    if (session != null) {
        //        session.handlePendingMessages(handlers)
        //    }
        //}
        //world.pulse()
        //synchronizer.synchronize(players, world.getNpcRepository())
    }

    override fun start() {
        executor.scheduleAtFixedRate(GamePulseHandler(this), PULSE_DELAY, PULSE_DELAY, TimeUnit.MILLISECONDS)
    }
}