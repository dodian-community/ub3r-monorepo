package net.dodian.uber.game.engine.systems.follow

import java.util.ArrayDeque
import kotlin.math.abs
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.engine.systems.pathing.AStarPathfindingAlgorithm
import net.dodian.uber.game.engine.systems.pathing.Heuristic
import net.dodian.uber.game.engine.systems.pathing.Node
import net.dodian.uber.game.engine.systems.pathing.collision.CollisionManager

object FollowRouting {
    private val collisionManager: CollisionManager
        get() = CollisionManager.global()
    private val pathfinding =
        AStarPathfindingAlgorithm(
            collision = { x, y, z, dx, dy -> collisionManager.traversable(x, y, z, dx, dy) },
            heuristic = Heuristic.EUCLIDEAN,
        )

    @JvmStatic
    fun enqueueRandomCardinalStep(follower: Client, z: Int): Boolean {
        val fx = follower.position.x
        val fy = follower.position.y
        val step = CARDINAL_OFFSETS.shuffled().firstOrNull { (dx, dy) -> isTraversableStep(fx, fy, dx, dy, z) } ?: return false
        val baseX = follower.mapRegionX * 8
        val baseY = follower.mapRegionY * 8
        follower.newWalkCmdSteps = 1
        follower.newWalkCmdIsRunning = false
        follower.newWalkCmdX[0] = fx + step.first - baseX
        follower.newWalkCmdY[0] = fy + step.second - baseY
        follower.tmpNWCX[0] = follower.newWalkCmdX[0]
        follower.tmpNWCY[0] = follower.newWalkCmdY[0]
        return true
    }

    @JvmStatic
    fun routeToEntityBoundary(
        follower: Client,
        targetX: Int,
        targetY: Int,
        targetSize: Int,
        z: Int,
        preferredDestination: Pair<Int, Int>? = null,
        running: Boolean = follower.buttonOnRun,
    ): Boolean {
        val normalizedTargetSize = targetSize.coerceAtLeast(1)
        val candidates = buildBoundaryCandidates(follower, targetX, targetY, normalizedTargetSize, preferredDestination)
        for (destination in candidates) {
            if (!isValidBoundaryDestination(destination, targetX, targetY, normalizedTargetSize, z)) {
                continue
            }
            val searchStart = System.nanoTime()
            val path = pathfinding.find(follower.position.x, follower.position.y, destination.first, destination.second, z)
            FollowPathfindingTelemetry.recordSearch(
                durationNanos = System.nanoTime() - searchStart,
                foundPath = path.isNotEmpty(),
            )
            if (path.isEmpty()) {
                continue
            }
            val validated = validatePath(follower.position.x, follower.position.y, z, path)
            if (validated.isEmpty()) {
                continue
            }
            applyRoute(follower, validated, running)
            return true
        }
        return false
    }

    @JvmStatic
    fun routeToExactTile(
        follower: Client,
        destinationX: Int,
        destinationY: Int,
        z: Int,
        running: Boolean = follower.buttonOnRun,
    ): Boolean {
        if (collisionManager.isTileBlocked(destinationX, destinationY, z)) {
            return false
        }
        val searchStart = System.nanoTime()
        val path = pathfinding.find(follower.position.x, follower.position.y, destinationX, destinationY, z)
        FollowPathfindingTelemetry.recordSearch(
            durationNanos = System.nanoTime() - searchStart,
            foundPath = path.isNotEmpty(),
        )
        if (path.isEmpty()) {
            return false
        }
        val validated = validatePath(follower.position.x, follower.position.y, z, path)
        if (validated.isEmpty()) {
            return false
        }
        applyRoute(follower, validated, running)
        return true
    }

    private fun buildBoundaryCandidates(
        follower: Client,
        targetX: Int,
        targetY: Int,
        targetSize: Int,
        preferredDestination: Pair<Int, Int>?,
    ): List<Pair<Int, Int>> {
        val minX = targetX
        val minY = targetY
        val maxX = targetX + targetSize - 1
        val maxY = targetY + targetSize - 1
        val ranked = ArrayList<Pair<Int, Int>>()
        if (preferredDestination != null && isBoundaryTile(preferredDestination, minX, minY, maxX, maxY)) {
            ranked += preferredDestination
        }
        for (x in (minX - 1)..(maxX + 1)) {
            for (y in (minY - 1)..(maxY + 1)) {
                val tile = x to y
                if (isBoundaryTile(tile, minX, minY, maxX, maxY)) {
                    ranked += tile
                }
            }
        }

        val unique = LinkedHashSet<Pair<Int, Int>>(ranked.size)
        unique.addAll(ranked)
        return unique.sortedWith(
            compareBy<Pair<Int, Int>>(
                { if (preferredDestination != null && it == preferredDestination) 0 else 1 },
                { distanceToFootprint(it.first, it.second, minX, minY, maxX, maxY) },
                { abs(it.first - follower.position.x) + abs(it.second - follower.position.y) },
            ),
        )
    }

    private fun isBoundaryTile(
        tile: Pair<Int, Int>,
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int,
    ): Boolean {
        val (x, y) = tile
        if (x in minX..maxX && y in minY..maxY) {
            return false
        }
        return x in (minX - 1)..(maxX + 1) && y in (minY - 1)..(maxY + 1)
    }

    private fun isValidBoundaryDestination(
        destination: Pair<Int, Int>,
        targetX: Int,
        targetY: Int,
        targetSize: Int,
        z: Int,
    ): Boolean {
        val (x, y) = destination
        if (collisionManager.isTileBlocked(x, y, z)) {
            return false
        }

        val minX = targetX
        val minY = targetY
        val maxX = targetX + targetSize - 1
        val maxY = targetY + targetSize - 1
        val nearestX = clamp(x, minX, maxX)
        val nearestY = clamp(y, minY, maxY)
        val stepX = nearestX - x
        val stepY = nearestY - y
        if (abs(stepX) > 1 || abs(stepY) > 1) {
            return false
        }
        return collisionManager.traversable(nearestX, nearestY, z, stepX, stepY)
    }

    private fun validatePath(startX: Int, startY: Int, z: Int, path: ArrayDeque<Node>): ArrayDeque<Node> {
        val validated = ArrayDeque<Node>(path.size)
        var currentX = startX
        var currentY = startY
        for (step in path) {
            val dx = (step.x - currentX).coerceIn(-1, 1)
            val dy = (step.y - currentY).coerceIn(-1, 1)
            if (dx == 0 && dy == 0) {
                continue
            }
            if (!collisionManager.traversable(step.x, step.y, z, dx, dy)) {
                break
            }
            validated.add(step)
            currentX = step.x
            currentY = step.y
        }
        return validated
    }

    private fun applyRoute(
        follower: Client,
        path: ArrayDeque<Node>,
        running: Boolean,
    ) {
        val baseX = follower.mapRegionX * 8
        val baseY = follower.mapRegionY * 8
        val steps = minOf(path.size, Player.WALKING_QUEUE_SIZE)
        follower.newWalkCmdSteps = steps
        follower.newWalkCmdIsRunning = running

        var index = 0
        for (step in path) {
            if (index >= steps) {
                break
            }
            follower.newWalkCmdX[index] = step.x - baseX
            follower.newWalkCmdY[index] = step.y - baseY
            follower.tmpNWCX[index] = follower.newWalkCmdX[index]
            follower.tmpNWCY[index] = follower.newWalkCmdY[index]
            index++
        }
    }

    private fun isTraversableStep(fx: Int, fy: Int, dx: Int, dy: Int, z: Int): Boolean {
        if (dx == 0 && dy == 0) {
            return true
        }
        return collisionManager.traversable(fx + dx, fy + dy, z, dx, dy)
    }

    private fun distanceToFootprint(x: Int, y: Int, minX: Int, minY: Int, maxX: Int, maxY: Int): Int {
        val dx = when {
            x < minX -> minX - x
            x > maxX -> x - maxX
            else -> 0
        }
        val dy = when {
            y < minY -> minY - y
            y > maxY -> y - maxY
            else -> 0
        }
        return dx + dy
    }

    private fun clamp(value: Int, min: Int, max: Int): Int = maxOf(min, minOf(max, value))

    private val CARDINAL_OFFSETS = listOf(-1 to 0, 1 to 0, 0 to 1, 0 to -1)
}

