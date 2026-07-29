package net.dodian.uber.game.objects.travel

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.api.content.ContentTiming
import net.dodian.uber.game.engine.systems.action.VerticalTransitionService

data class VerticalTravelStyle(
    val animationId: Int = -1,
    val delayMs: Long,
)

object VerticalTravelStyles {
    @JvmField
    val LADDER = VerticalTravelStyle(animationId = 828, delayMs = 200L)

    @JvmField
    val STAIRS = VerticalTravelStyle(delayMs = 125L)

    @JvmField
    val TRAPDOOR = VerticalTravelStyle(delayMs = 125L)
}

object VerticalTravel {
    @JvmStatic
    fun start(client: Client, destination: Position, style: VerticalTravelStyle): Boolean {
        if (client.disconnected || VerticalTransitionService.isActive(client)) {
            return true
        }
        client.resetWalkingQueue()
        if (style.animationId >= 0) {
            client.performAnimation(style.animationId, 0)
        }
        val token = VerticalTransitionService.begin(client, style.delayMs)
        val debugContext =
            "player=${client.playerName} token=$token destination=$destination state=${VerticalTransitionService.debugSummary(client)}"
        ContentTiming.scheduleGameThread("vertical-travel", style.delayMs, debugContext) {
            VerticalTransitionService.finish(client, token, destination)
        }
        return true
    }
}
