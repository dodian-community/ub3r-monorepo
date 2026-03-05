package net.dodian.uber.game.model.chunk

import java.util.Comparator
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Player
import kotlin.math.abs
import kotlin.math.max

class ChunkPlayerComparator(
    private val viewer: Player,
) : Comparator<Player> {
    override fun compare(left: Player, right: Player): Int {
        if (left === right) {
            return 0
        }

        var leftWeight = 0
        var rightWeight = 0

        val viewerPos = viewer.position
        val leftDistance = computeLongestDistance(viewerPos, left.position)
        val rightDistance = computeLongestDistance(viewerPos, right.position)

        if (leftDistance < rightDistance) {
            leftWeight += 3
        } else if (rightDistance < leftDistance) {
            rightWeight += 3
        }

        val weightCompare = Integer.compare(rightWeight, leftWeight)
        if (weightCompare != 0) {
            return weightCompare
        }
        return Integer.compare(left.slot, right.slot)
    }

    private fun computeLongestDistance(a: Position?, b: Position?): Int {
        if (a == null || b == null) {
            return Int.MAX_VALUE
        }
        val dx = abs(a.x - b.x)
        val dy = abs(a.y - b.y)
        return max(dx, dy)
    }
}
