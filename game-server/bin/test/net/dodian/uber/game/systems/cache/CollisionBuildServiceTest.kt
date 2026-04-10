package net.dodian.uber.game.systems.cache

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.systems.pathing.collision.CollisionManager
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CollisionBuildServiceTest {
    @Test
    fun `solid non-walkable object marks footprint blocked`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1000,
            x = 3200,
            y = 3200,
            z = 0,
            type = 10,
            rotation = 0,
            sizeX = 2,
            sizeY = 1,
            solid = true,
            walkable = false,
        )

        assertFalse(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `default object blocks movement when solid even if walkable is true`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1001,
            x = 3200,
            y = 3200,
            z = 0,
            type = 10,
            rotation = 0,
            sizeX = 2,
            sizeY = 1,
            solid = true,
            walkable = true,
        )

        assertFalse(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `wall type blocks movement even when definition reports non-solid`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1010,
            x = 3200,
            y = 3200,
            z = 0,
            type = 0,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = false,
            walkable = true,
        )

        assertFalse(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `diagonal wall type blocks diagonal traversal for fence-style lines`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1013,
            x = 3200,
            y = 3200,
            z = 0,
            type = 1,
            rotation = 1,
            sizeX = 1,
            sizeY = 1,
            solid = false,
            walkable = true,
        )

        assertFalse(manager.canMove(3200, 3200, 3201, 3201, 0, 1, 1))
    }

    @Test
    fun `ground decoration blocks only when interactive and solid`() {
        val manager = CollisionManager()
        val service = CollisionBuildService(manager)

        service.applyObject(
            id = 1011,
            x = 3200,
            y = 3200,
            z = 0,
            type = 22,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
            hasActions = false,
        )
        assertTrue(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))

        service.applyObject(
            id = 1012,
            x = 3201,
            y = 3200,
            z = 0,
            type = 22,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
            hasActions = true,
        )
        assertFalse(manager.canMove(3200, 3200, 3201, 3200, 0, 1, 1))
    }

    @Test
    fun `diagonal wall type 9 blocks movement even for non-solid definitions`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1014,
            x = 3200,
            y = 3200,
            z = 0,
            type = 9,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = false,
            walkable = true,
        )

        assertFalse(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `straight wall object blocks traversal in its facing direction`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1002,
            x = 3200,
            y = 3200,
            z = 0,
            type = 0,
            rotation = 2,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
        )

        assertFalse(manager.canMove(3200, 3200, 3201, 3200, 0, 1, 1))
        assertTrue(manager.canMove(3200, 3200, 3200, 3201, 0, 1, 1))
    }

    @Test
    fun `bridge tile lowers blocked terrain collision plane`() {
        val manager = CollisionManager()
        val service = CollisionBuildService(manager)

        val table =
            MapIndexTable(listOf(MapIndexEntry(regionId = 42, landscapeArchiveId = 43, objectArchiveId = 44))).withDecodedData(
                tileGrids = mapOf(42 to gridWithBridgeBlockedTile()),
                objects = emptyList(),
            )

        service.rebuild(table)

        assertFalse(manager.canMove(0, 2688, 1, 2688, 0, 1, 1))
        assertTrue(manager.canMove(0, 2688, 1, 2688, 1, 1, 1))
    }

    @Test
    fun `decoded solid object definitions are applied during rebuild`() {
        val manager = CollisionManager()
        val service = CollisionBuildService(manager)

        GameObjectData.addDefinition(
            GameObjectData(
                id = 2000,
                name = "Blocking fixture object",
                description = "fixture",
                sizeX = 1,
                sizeY = 1,
                solid = true,
                walkable = false,
                hasActionsFlag = true,
                unknownValue = false,
                walkType = 0,
            ),
        )

        val table =
            MapIndexTable(listOf(MapIndexEntry(regionId = 42, landscapeArchiveId = 43, objectArchiveId = 44))).withDecodedData(
                tileGrids = mapOf(42 to emptyGrid(regionId = 42)),
                objects = listOf(DecodedMapObject(objectId = 2000, x = 0, y = 2688, plane = 0, type = 10, rotation = 0, regionId = 42)),
            )

        service.rebuild(table)

        assertFalse(manager.canMove(0, 2687, 0, 2688, 0, 1, 1))
    }

    private fun gridWithBridgeBlockedTile(): DecodedMapTileGrid {
        val tiles = Array(4) { plane -> Array(64) { x -> Array(64) { y -> emptyTile(x, y, plane) } } }
        tiles[1][1][0] = emptyTile(1, 0, 1).copy(attributes = DecodedMapTile.BLOCKED or DecodedMapTile.BRIDGE)
        return DecodedMapTileGrid(regionId = 42, tiles = tiles)
    }

    private fun emptyGrid(regionId: Int): DecodedMapTileGrid {
        val tiles = Array(4) { plane -> Array(64) { x -> Array(64) { y -> emptyTile(x, y, plane) } } }
        return DecodedMapTileGrid(regionId = regionId, tiles = tiles)
    }

    private fun emptyTile(x: Int, y: Int, plane: Int): DecodedMapTile =
        DecodedMapTile(
            offsetX = x,
            offsetY = y,
            plane = plane,
            height = 0,
            overlay = 0,
            overlayType = 0,
            overlayOrientation = 0,
            attributes = 0,
            underlay = 0,
        )
}
