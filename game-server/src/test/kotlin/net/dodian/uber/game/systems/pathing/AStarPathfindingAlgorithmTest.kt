package net.dodian.uber.game.systems.pathing

import net.dodian.uber.game.systems.pathing.collision.CollisionManager
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

    private class FakeCollisionManager : PathingCollision {
        override fun traversable(x: Int, y: Int, z: Int, dx: Int, dy: Int): Boolean = true
    }
}
