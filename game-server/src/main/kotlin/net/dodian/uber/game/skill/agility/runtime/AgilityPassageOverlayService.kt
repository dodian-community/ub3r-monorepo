package net.dodian.uber.game.skill.agility.runtime

import kotlin.math.abs
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.interaction.PersonalPassageService

object AgilityPassageOverlayService {
    private const val PASSAGE_GRANT_BUFFER_MS = 1_200L

    @JvmStatic
    fun grantForDelta(
        player: Client,
        deltaX: Int,
        deltaY: Int,
        durationMs: Long,
    ) {
        val edges = pathEdges(player.position.copy(), deltaX, deltaY)
        if (edges.isEmpty()) {
            return
        }
        PersonalPassageService.grantBidirectionalEdges(
            player = player,
            edges = edges,
            durationMs = durationMs + PASSAGE_GRANT_BUFFER_MS,
        )
    }

    private fun pathEdges(
        start: Position,
        deltaX: Int,
        deltaY: Int,
    ): List<Pair<Position, Position>> {
        val steps = maxOf(abs(deltaX), abs(deltaY))
        if (steps <= 0) {
            return emptyList()
        }
        val edges = ArrayList<Pair<Position, Position>>(steps)
        val xSign = deltaX.compareTo(0)
        val ySign = deltaY.compareTo(0)
        var remainingX = abs(deltaX)
        var remainingY = abs(deltaY)
        var from = start.copy()
        repeat(steps) {
            val stepX = if (remainingX > 0) xSign else 0
            val stepY = if (remainingY > 0) ySign else 0
            val to = Position(from.x + stepX, from.y + stepY, from.z)
            edges += from to to
            from = to
            if (remainingX > 0) remainingX--
            if (remainingY > 0) remainingY--
        }
        return edges
    }
}
