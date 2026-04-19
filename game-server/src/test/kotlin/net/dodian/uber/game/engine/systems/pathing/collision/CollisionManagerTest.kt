package net.dodian.uber.game.engine.systems.pathing.collision

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CollisionManagerTest {
    @Test
    fun `open tile permits east traversal`() {
        val manager = CollisionManager()

        val canMove = manager.canMove(3200, 3200, 3201, 3200, 0, 1, 1)

        assertTrue(canMove)
    }

    @Test
    fun `blocked wall prevents east traversal`() {
        val manager = CollisionManager()
        manager.flagWall(3200, 3200, 0, eastBlocked = true)

        val canMove = manager.canMove(3200, 3200, 3201, 3200, 0, 1, 1)

        assertFalse(canMove)
    }

    @Test
    fun `solid tile blocks footprint movement`() {
        val manager = CollisionManager()
        manager.flagSolid(3201, 3200, 0)

        val canMove = manager.canMove(3200, 3200, 3201, 3200, 0, 1, 1)

        assertFalse(canMove)
    }

    @Test
    fun `east wall also prevents north east corner clipping`() {
        val manager = CollisionManager()
        manager.flagWall(3200, 3200, 0, eastBlocked = true)

        val canMove = manager.canMove(3200, 3200, 3201, 3201, 0, 1, 1)

        assertFalse(canMove)
    }
}
