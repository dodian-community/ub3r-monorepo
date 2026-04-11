package net.dodian.uber.game.engine.systems.pathing

import net.dodian.uber.game.engine.systems.pathing.collision.CollisionManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AStarPathfindingAlgorithmTest {
    @Test
    fun `astar returns non-empty path in unobstructed grid`() {
        val algorithm = AStarPathfindingAlgorithm(FakeCollisionManager())
        val path = algorithm.find(3200, 3200, 3205, 3205, 0)
        assertTrue(path.isNotEmpty())
    }

    @Test
    fun `astar returns empty path when destination tile is blocked`() {
        val collision = CollisionManager().apply { flagSolid(3201, 3200, 0) }
        val algorithm = AStarPathfindingAlgorithm(PathingCollision { x, y, z, dx, dy -> collision.traversable(x, y, z, dx, dy) })

        val path = algorithm.find(3200, 3200, 3201, 3200, 0)

        assertFalse(path.isNotEmpty())
    }

    @Test
    fun `astar produces deterministic route for fixed obstacle layout`() {
        val collision = CollisionManager().apply {
            flagSolid(3201, 3200, 0)
            flagSolid(3201, 3201, 0)
            flagSolid(3201, 3202, 0)
        }
        val algorithm = AStarPathfindingAlgorithm(PathingCollision { x, y, z, dx, dy -> collision.traversable(x, y, z, dx, dy) })

        val first = algorithm.find(3200, 3200, 3203, 3201, 0).map { it.x to it.y }
        val second = algorithm.find(3200, 3200, 3203, 3201, 0).map { it.x to it.y }

        assertTrue(first.isNotEmpty())
        assertEquals(first, second)
        assertEquals(3203 to 3201, first.last())
    }

    @Test
    fun `astar path remains step-valid around obstacles`() {
        val collision = CollisionManager().apply {
            flagSolid(3201, 3200, 0)
            flagSolid(3202, 3201, 0)
        }
        val algorithm = AStarPathfindingAlgorithm(PathingCollision { x, y, z, dx, dy -> collision.traversable(x, y, z, dx, dy) })

        val path = algorithm.find(3200, 3200, 3203, 3202, 0).toList()

        assertTrue(path.isNotEmpty())
        var prevX = 3200
        var prevY = 3200
        for (step in path) {
            val dx = step.x - prevX
            val dy = step.y - prevY
            assertTrue(dx in -1..1 && dy in -1..1 && !(dx == 0 && dy == 0))
            assertTrue(collision.traversable(step.x, step.y, 0, dx, dy))
            prevX = step.x
            prevY = step.y
        }
        val end = path.last()
        assertEquals(3203, end.x)
        assertEquals(3202, end.y)
    }

    private class FakeCollisionManager : PathingCollision {
        override fun traversable(x: Int, y: Int, z: Int, dx: Int, dy: Int): Boolean = true
    }
}
