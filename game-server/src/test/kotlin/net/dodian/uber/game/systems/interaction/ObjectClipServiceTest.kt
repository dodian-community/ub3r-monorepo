package net.dodian.uber.game.systems.interaction

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.`object`.DoorRegistry
import net.dodian.uber.game.model.`object`.RS2Object
import net.dodian.uber.game.systems.pathing.collision.CollisionManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObjectClipServiceTest {
    @Test
    fun `bootstrap startup overlays applies world objects and doors to global collision`() {
        val snapshot = DoorRegistrySnapshot.capture()
        val collision = CollisionManager.global()
        collision.clear()
        ObjectClipService.clearForTests()

        try {
            GameObjectData.addDefinition(blockingDefinition(2000))
            GameObjectData.addDefinition(blockingDefinition(2001))

            DoorRegistry.doorX = intArrayOf(20)
            DoorRegistry.doorY = intArrayOf(20)
            DoorRegistry.doorId = intArrayOf(2001)
            DoorRegistry.doorHeight = intArrayOf(0)
            DoorRegistry.doorFaceOpen = intArrayOf(3)
            DoorRegistry.doorFaceClosed = intArrayOf(3)
            DoorRegistry.doorFace = intArrayOf(3)
            DoorRegistry.doorState = intArrayOf(0)

            ObjectClipService.bootstrapStartupOverlays(listOf(RS2Object(2000, 15, 15, 10, 0)))

            assertTrue(collision.isTileBlocked(15, 15, 0))
            assertFalse(collision.canMove(20, 19, 20, 20, 0, 1, 1))
        } finally {
            ObjectClipService.clearForTests()
            collision.clear()
            snapshot.restore()
        }
    }

    @Test
    fun `apply decoded object replaces prior collision at same tile`() {
        val collision = CollisionManager.global()
        collision.clear()
        ObjectClipService.clearForTests()

        try {
            GameObjectData.addDefinition(blockingDefinition(2002))
            val position = Position(30, 30, 0)
            val definition = GameObjectData.forId(2002)

            ObjectClipService.applyDecodedObject(position, 2002, 0, 3, definition)
            assertFalse(collision.canMove(30, 29, 30, 30, 0, 1, 1))

            ObjectClipService.applyDecodedObject(position, 2002, 0, 0, definition)
            assertTrue(collision.canMove(30, 29, 30, 30, 0, 1, 1))
            assertFalse(collision.canMove(29, 30, 30, 30, 0, 1, 1))
        } finally {
            ObjectClipService.clearForTests()
            collision.clear()
        }
    }

    @Test
    fun `runtime decoded wall type remains blocked even when definition is non-solid`() {
        val collision = CollisionManager.global()
        collision.clear()
        ObjectClipService.clearForTests()

        try {
            GameObjectData.addDefinition(nonSolidDefinition(2003))
            val position = Position(40, 40, 0)
            val definition = GameObjectData.forId(2003)

            ObjectClipService.applyDecodedObject(position, 2003, 0, 0, definition)

            assertFalse(collision.canMove(39, 40, 40, 40, 0, 1, 1))
        } finally {
            ObjectClipService.clearForTests()
            collision.clear()
        }
    }

    @Test
    fun `static removal override clears cache collision for deleted door tile`() {
        val collision = CollisionManager.global()
        collision.clear()
        ObjectClipService.clearForTests()

        try {
            GameObjectData.addDefinition(blockingDefinition(2100))
            val position = Position(50, 50, 0)

            ObjectClipService.applyDecodedObject(position, 2100, 0, 0, GameObjectData.forId(2100))
            assertFalse(collision.canMove(49, 50, 50, 50, 0, 1, 1))

            ObjectClipService.applyStaticOverrides(
                listOf(
                    StaticObjectOverride(position, replacementObjectId = -1, replacementFace = -1, replacementType = 0),
                ),
            )

            assertTrue(collision.canMove(49, 50, 50, 50, 0, 1, 1))
            assertTrue(collision.canMove(50, 49, 50, 50, 0, 1, 1))
        } finally {
            ObjectClipService.clearForTests()
            collision.clear()
        }
    }

    @Test
    fun `static replacement override reapplies replacement collision after clearing old tile`() {
        val collision = CollisionManager.global()
        collision.clear()
        ObjectClipService.clearForTests()

        try {
            GameObjectData.addDefinition(blockingDefinition(2101))
            GameObjectData.addDefinition(blockingDefinition(2102))
            val position = Position(60, 60, 0)

            ObjectClipService.applyDecodedObject(position, 2101, 0, 0, GameObjectData.forId(2101))
            assertFalse(collision.canMove(59, 60, 60, 60, 0, 1, 1))
            assertTrue(collision.canMove(60, 59, 60, 60, 0, 1, 1))

            ObjectClipService.applyStaticOverrides(
                listOf(
                    StaticObjectOverride(position, replacementObjectId = 2102, replacementFace = 3, replacementType = 0),
                ),
            )

            assertTrue(collision.canMove(59, 60, 60, 60, 0, 1, 1))
            assertFalse(collision.canMove(60, 59, 60, 60, 0, 1, 1))
        } finally {
            ObjectClipService.clearForTests()
            collision.clear()
        }
    }

    @Test
    fun `static override registry includes migrated deleted door positions`() {
        val positions = StaticObjectOverrides.all().map { it.position.x to it.position.y }.toSet()

        assertEquals(
            setOf(
                2669 to 2713,
                2713 to 3483,
                2716 to 3472,
                2594 to 3102,
                2816 to 3438,
            ),
            positions,
        )
    }

    private fun blockingDefinition(id: Int): GameObjectData =
        GameObjectData(
            id = id,
            name = "Blocking object $id",
            description = "fixture",
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
            hasActionsFlag = true,
            unknownValue = false,
            walkType = 0,
        )

    private fun nonSolidDefinition(id: Int): GameObjectData =
        GameObjectData(
            id = id,
            name = "Non solid wall $id",
            description = "fixture",
            sizeX = 1,
            sizeY = 1,
            solid = false,
            walkable = true,
            hasActionsFlag = true,
            unknownValue = false,
            walkType = 0,
        )

    private class DoorRegistrySnapshot(
        private val doorX: IntArray,
        private val doorY: IntArray,
        private val doorId: IntArray,
        private val doorHeight: IntArray,
        private val doorFaceOpen: IntArray,
        private val doorFaceClosed: IntArray,
        private val doorFace: IntArray,
        private val doorState: IntArray,
    ) {
        fun restore() {
            DoorRegistry.doorX = doorX
            DoorRegistry.doorY = doorY
            DoorRegistry.doorId = doorId
            DoorRegistry.doorHeight = doorHeight
            DoorRegistry.doorFaceOpen = doorFaceOpen
            DoorRegistry.doorFaceClosed = doorFaceClosed
            DoorRegistry.doorFace = doorFace
            DoorRegistry.doorState = doorState
        }

        companion object {
            fun capture(): DoorRegistrySnapshot =
                DoorRegistrySnapshot(
                    doorX = DoorRegistry.doorX.copyOf(),
                    doorY = DoorRegistry.doorY.copyOf(),
                    doorId = DoorRegistry.doorId.copyOf(),
                    doorHeight = DoorRegistry.doorHeight.copyOf(),
                    doorFaceOpen = DoorRegistry.doorFaceOpen.copyOf(),
                    doorFaceClosed = DoorRegistry.doorFaceClosed.copyOf(),
                    doorFace = DoorRegistry.doorFace.copyOf(),
                    doorState = DoorRegistry.doorState.copyOf(),
                )
        }
    }
}
