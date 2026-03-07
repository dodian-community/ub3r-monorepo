package net.dodian.uber.game.content.objects.impl.travel

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.loop.GameThreadTimers

sealed interface VerticalTravelCompletion {
    data class QueuedDestination(
        val destination: Position,
    ) : VerticalTravelCompletion

    data class LegacyStair(
        val stairs: Int,
        val skillX: Int,
        val skillY: Int,
        val stairDistance: Int,
        val stairDistanceAdd: Int = 0,
    ) : VerticalTravelCompletion
}

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
    fun start(client: Client, completion: VerticalTravelCompletion, style: VerticalTravelStyle): Boolean {
        if (client.disconnected || client.isVerticalTransitionActive()) {
            return true
        }
        client.resetWalkingQueue()
        if (style.animationId >= 0) {
            client.requestAnim(style.animationId, 0)
        }
        val token = client.beginVerticalTransition(style.delayMs)
        val debugContext =
            "player=${client.playerName} token=$token completion=$completion state=${client.verticalTransitionDebugSummary()}"
        GameThreadTimers.schedule("vertical-travel", style.delayMs, debugContext) {
            when (completion) {
                is VerticalTravelCompletion.QueuedDestination ->
                    client.finishVerticalTransition(token, completion.destination)

                is VerticalTravelCompletion.LegacyStair ->
                    client.finishVerticalTransition(
                        token,
                        completion.stairs,
                        completion.skillX,
                        completion.skillY,
                        completion.stairDistance,
                        completion.stairDistanceAdd,
                    )
            }
        }
        return true
    }
}
