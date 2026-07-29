package net.dodian.uber.game.engine.systems.action

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

/** Owns the short-lived transition token used by ladders, stairs and trapdoors. */
object VerticalTransitionService {
    private data class State(var sequence: Long = 0, var activeToken: Long = 0, var untilMillis: Long = 0)
    private const val STATE_KEY = "vertical-transition"
    private fun state(client: Client): State = client.contentRuntimeState.getPluginAttribute<State>(STATE_KEY)
        ?: State().also { client.contentRuntimeState.putPluginAttribute(STATE_KEY, it) }

    @JvmStatic fun begin(client: Client, delayMs: Long): Long {
        client.resetWalkingQueue()
        val state = state(client)
        state.activeToken = ++state.sequence
        state.untilMillis = System.currentTimeMillis() + delayMs.coerceAtLeast(0)
        client.applyWalkBlockMs(delayMs.coerceAtLeast(0))
        return state.activeToken
    }

    @JvmStatic fun isActive(client: Client): Boolean = state(client).let { it.activeToken != 0L && it.untilMillis > System.currentTimeMillis() }
    @JvmStatic fun clear(client: Client) { state(client).apply { activeToken = 0; untilMillis = 0 } }
    @JvmStatic fun queueTransport(client: Client, destination: Position) {
        client.resetActionTeleport()
        client.teleportToX = destination.x
        client.teleportToY = destination.y
        client.teleportToZ = destination.z
    }
    @JvmStatic fun finish(client: Client, token: Long, destination: Position) {
        if (state(client).activeToken != token || client.disconnected) return
        queueTransport(client, destination)
        clear(client)
    }
    @JvmStatic fun debugSummary(client: Client): String = state(client).let {
        "token=${it.activeToken},until=${it.untilMillis},tele=(${client.teleportToX},${client.teleportToY},${client.teleportToZ}),pos=${client.position}"
    }
}
