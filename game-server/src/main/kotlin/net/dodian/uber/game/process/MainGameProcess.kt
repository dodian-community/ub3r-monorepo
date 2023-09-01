package net.dodian.uber.game.process

import net.dodian.uber.event.EventBus
import net.dodian.uber.event.impl.GameProcessEvent
import net.dodian.uber.game.coroutines.GameCoroutineScope
import net.dodian.uber.game.model.mob.list.ClientList
import net.dodian.uber.game.model.mob.list.PlayerList

class MainGameProcess(
    @GameProcessScope private val coroutineScope: GameCoroutineScope,
    private val clients: ClientList,
    private val players: PlayerList,
    private val eventBus: EventBus,
    private val clock: WorldClock
) : GameProcess {

    override fun startUp() {
        eventBus.publish(GameProcessEvent.BootUp)
    }

    override fun shutDown() {
        coroutineScope.cancel()
    }

    override fun cycle() {
        startCycle()
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
        // TODO: !
        //movementProcess.execute()
        //postMovementProcess.execute()
        /* info task should be last step in this cycle */
        //gpiTask.execute()
    }

    private fun clientOutput() {
        clients.flushDownstream()
    }

    private fun endCycle() {
        eventBus.publish(GameProcessEvent.EndCycle)
    }

    private fun PlayerList.advanceCoroutineScope() {
        // TODO: !
        //forEach { player -> player.coroutineScope.advance() }
    }

    private fun ClientList.readChannels() {
        forEach { client -> client.channel.read() }
    }

    private fun PlayerList.readUpstream() {
        forEach { player ->
            // TODO: !
            //val upstream = player.upstream
            //upstreamTask.readAll(player, upstream)
            //upstream.clear()
        }
    }

    private fun PlayerList.publishEvents() {
        forEach { player ->
            // TODO: !
            //val events = player.events
            //events.publishAll(player, eventBus)
            //events.clear()
        }
    }

    private fun ClientList.flushDownstream() {
        forEach { client ->
            // TODO: !
            //val downstream = client.player.downstream
            //downstream.flush(client.channel)
            //downstream.clear()
        }
    }
}