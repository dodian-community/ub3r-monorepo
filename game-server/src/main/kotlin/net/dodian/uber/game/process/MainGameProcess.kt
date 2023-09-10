package net.dodian.uber.game.process

import net.dodian.uber.event.EventBus
import net.dodian.uber.event.impl.GameProcessEvent
import net.dodian.uber.event.impl.PlayerSessionEvent
import net.dodian.uber.game.coroutines.GameCoroutineScope
import net.dodian.uber.game.extensions.sendInitialMessages
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.mob.list.ClientList
import net.dodian.uber.game.model.mob.list.PlayerList
import net.dodian.uber.game.session.PlayerManager
import net.dodian.uber.game.sync.task.PlayerSynchronizationTask
import net.dodian.uber.net.codec.login.STATUS_ACCOUNT_ONLINE
import net.dodian.uber.net.codec.login.STATUS_GAME_UPDATED
import net.dodian.uber.net.codec.login.STATUS_SERVER_FULL

private const val DE_REGISTRATIONS_PER_CYCLE = 50
private const val REGISTRATIONS_PER_CYCLE = 25

class MainGameProcess(
    @GameProcessScope private val coroutineScope: GameCoroutineScope,
    private val eventBus: EventBus,
    private val clients: ClientList,
    private val players: PlayerList,
    private val clock: WorldClock,
    private val playerHandler: PlayerManager,
    private val playerSyncTask: PlayerSynchronizationTask
) : GameProcess {

    override fun startUp() {
        eventBus.publish(GameProcessEvent.BootUp)
    }

    override fun shutDown() {
        coroutineScope.cancel()
    }

    override fun cycle() {
        startCycle()
        worldCycle()
        clientInput()
        playerCycle()
        clientOutput()
        endCycle()
    }

    private fun startCycle() {
        eventBus.publish(GameProcessEvent.StartCycle)
    }

    private fun worldCycle() {
        clock.tick++
        coroutineScope.advance()
        players.advanceCoroutineScope()
    }

    private fun clientInput() {
        clients.readChannels()
        players.readUpstream()
    }

    private fun playerCycle() {
        players.publishEvents()
        playerSyncTask.execute()
        // TODO: !
        //movementProcess.execute()
        //postMovementProcess.execute()
        /* info task should be last step in this cycle */
        //gpiTask.execute()
    }

    private fun clientOutput() {
        players.flushDownstream()
    }

    private fun endCycle() {
        finalizeRegistrations()
        eventBus.publish(GameProcessEvent.EndCycle)
    }

    private fun PlayerList.advanceCoroutineScope() {
        forEach { player -> player.coroutineScope.advance() }
    }

    private fun ClientList.readChannels() {
        forEach { client -> client.channel.read() }
    }

    private fun PlayerList.readUpstream() {
        forEach { player ->
            val upstream = player.upstream
            // TODO: !
            //upstreamTask.readAll(player, upstream)
            upstream.clear()
        }
    }

    private fun PlayerList.publishEvents() {
        forEach { player ->
            player.session.handlePendingMessages(handlers)
            val events = player.events
            events.publishAll(player, eventBus)
            events.clear()
        }
    }

    private fun PlayerList.flushDownstream() {
        forEach { player ->
            val downstream = player.downstream
            downstream.flush(player.session.channel)
            downstream.clear()
        }
    }

    private fun finalizeRegistrations() {
        for (i in 0 until REGISTRATIONS_PER_CYCLE) {
            val request = playerHandler.newPlayers.poll() ?: continue

            val player = request.player

            if (playerHandler.isPlayerOnline(player)) {
                request.session.sendLoginFailure(STATUS_ACCOUNT_ONLINE)
            } else if (playerHandler.isFull) {
                request.session.sendLoginFailure(STATUS_SERVER_FULL)
            } else {
                eventBus.publish(player, PlayerSessionEvent.Finalize)
                request.session.sendLoginSuccess(player)
                finalizePlayerRegistration(player)
            }
        }
    }

    private fun finalizePlayerRegistration(player: Player) {
        playerHandler.register(player)

        if (!player.session.isReconnecting)
            (player as Client).sendInitialMessages()
    }
}