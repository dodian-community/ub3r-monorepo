package net.dodian.uber.game.systems.pathing

import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.min

class AStarPathfindingAlgorithm(
    private val collision: PathingCollision,
    private val heuristic: Heuristic = Heuristic.MANHATTAN,
) : PathfindingAlgorithm {
    override fun find(srcX: Int, srcY: Int, dstX: Int, dstY: Int, z: Int): ArrayDeque<Node> {
        if (srcX == dstX && srcY == dstY) {
            return ArrayDeque()
        }

        val minX = min(srcX, dstX) - SEARCH_MARGIN
        val maxX = max(srcX, dstX) + SEARCH_MARGIN
        val minY = min(srcY, dstY) - SEARCH_MARGIN
        val maxY = max(srcY, dstY) + SEARCH_MARGIN

        val open = mutableListOf(Node(srcX, srcY, z, 0, heuristic.estimate(srcX, srcY, dstX, dstY)))
        val bestCosts = HashMap<Long, Int>()
        val closed = HashSet<Long>()
        bestCosts[key(srcX, srcY, z)] = 0
        var expansions = 0

        while (open.isNotEmpty()) {
            if (expansions++ >= MAX_EXPANSIONS) {
                return ArrayDeque()
            }

            var bestIndex = 0
            var bestNode = open[0]
            for (index in 1 until open.size) {
                val candidate = open[index]
                if (candidate.f < bestNode.f || candidate.f == bestNode.f && candidate.h < bestNode.h) {
                    bestNode = candidate
                    bestIndex = index
                }
            }

            val current = open.removeAt(bestIndex)
            val currentKey = key(current.x, current.y, current.z)
            if (!closed.add(currentKey)) {
                continue
            }
            if (current.x == dstX && current.y == dstY) {
                return buildPath(current)
            }

            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) {
                        continue
                    }

                    val nextX = current.x + dx
                    val nextY = current.y + dy
                    if (nextX < minX || nextX > maxX || nextY < minY || nextY > maxY) {
                        continue
                    }
                    if (!collision.traversable(nextX, nextY, z, dx, dy)) {
                        continue
                    }

                    val nextCost = current.g + if (dx == 0 || dy == 0) 10 else 14
                    val nextKey = key(nextX, nextY, z)
                    val recordedCost = bestCosts[nextKey]
                    if (recordedCost != null && nextCost >= recordedCost) {
                        continue
                    }

                    bestCosts[nextKey] = nextCost
                    val nextHeuristic = heuristic.estimate(nextX, nextY, dstX, dstY)
                    open.add(Node(nextX, nextY, z, nextCost, nextHeuristic, current))
                }
            }
        }

        return ArrayDeque()
    }

    private fun buildPath(end: Node): ArrayDeque<Node> {
        val path = ArrayDeque<Node>()
        var current: Node? = end
        while (current?.parent != null) {
            path.addFirst(current)
            current = current.parent
        }
        return path
    }

    private fun key(x: Int, y: Int, z: Int): Long = (x.toLong() shl 42) xor (y.toLong() shl 20) xor z.toLong()

    private companion object {
        const val SEARCH_MARGIN = 24
        const val MAX_EXPANSIONS = 8192
    }
}
