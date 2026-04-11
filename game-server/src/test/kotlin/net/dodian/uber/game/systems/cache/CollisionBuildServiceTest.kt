package net.dodian.uber.game.systems.cache

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.systems.pathing.collision.CollisionFlag
import net.dodian.uber.game.systems.pathing.collision.CollisionManager
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `default object blocks movement when solid walkable and interactive`() {
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
    fun `type 10 solid non-interactive blocks movement`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1022,
            x = 3200,
            y = 3200,
            z = 0,
            type = 10,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
            hasActions = false,
        )

        assertFalse(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `type 11 solid non-interactive blocks movement`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1023,
            x = 3200,
            y = 3200,
            z = 0,
            type = 11,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
            hasActions = false,
        )

        assertFalse(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `wall type does not block movement when definition reports non-solid`() {
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

        assertTrue(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `diagonal wall type does not block diagonal traversal when definition is non-solid`() {
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

        assertTrue(manager.canMove(3200, 3200, 3201, 3201, 0, 1, 1))
    }

    @Test
    fun `wall type 3 does not block diagonal traversal when definition is non-solid`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1021,
            x = 3200,
            y = 3200,
            z = 0,
            type = 3,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = false,
            walkable = true,
        )

        assertTrue(manager.canMove(3199, 3199, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `wall decoration type 4 non-solid does not block movement`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1018,
            x = 3200,
            y = 3200,
            z = 0,
            type = 4,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = false,
            walkable = true,
        )

        assertTrue(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `wall decoration type 4 solid still does not block movement`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1019,
            x = 3200,
            y = 3200,
            z = 0,
            type = 4,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
        )

        assertTrue(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `wall decoration type 8 solid still does not block movement`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1020,
            x = 3200,
            y = 3200,
            z = 0,
            type = 8,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
        )

        assertTrue(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `ground decoration blocks only when blockWalk equals one`() {
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
            blockWalk = 2,
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
            blockWalk = 1,
        )
        assertFalse(manager.canMove(3200, 3200, 3201, 3200, 0, 1, 1))
    }

    @Test
    fun `diagonal wall type 9 does not block movement for non-solid definitions`() {
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

        assertTrue(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `diagonal wall type 9 blocks movement when definition is solid`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1017,
            x = 3200,
            y = 3200,
            z = 0,
            type = 9,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
        )

        assertFalse(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `roofing types do not block movement when definition is non-solid`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1015,
            x = 3200,
            y = 3200,
            z = 0,
            type = 12,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = false,
            walkable = true,
        )

        assertTrue(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `roofing types block movement when definition is solid`() {
        val manager = CollisionManager()

        CollisionBuildService(manager).applyObject(
            id = 1016,
            x = 3200,
            y = 3200,
            z = 0,
            type = 12,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
        )

        assertFalse(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
    }

    @Test
    fun `luna interactable footprint does not rotate size on rotation one`() {
        val rotated =
            CollisionBuildService.resolveFootprint(
                type = 10,
                normalizedRotation = 1,
                sizeX = 2,
                sizeY = 1,
                mode = CollisionBuildService.FootprintMode.ROTATED,
            )
        val luna =
            CollisionBuildService.resolveFootprint(
                type = 10,
                normalizedRotation = 1,
                sizeX = 2,
                sizeY = 1,
                mode = CollisionBuildService.FootprintMode.LUNA_UNROTATED_INTERACTABLE,
            )

        assertTrue(rotated.first == 1 && rotated.second == 2)
        assertTrue(luna.first == 2 && luna.second == 1)
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

    @Test
    fun `block range false keeps movement blocking but drops projectile flags`() {
        val manager = CollisionManager()
        val service = CollisionBuildService(manager)

        service.applyObject(
            id = 3000,
            x = 3200,
            y = 3200,
            z = 0,
            type = 10,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
            blockWalk = 2,
            blockRange = false,
            breakRouteFinding = false,
        )

        assertFalse(manager.canMove(3199, 3200, 3200, 3200, 0, 1, 1))
        val flags = manager.getFlags(3200, 3200, 0)
        assertTrue(flags and CollisionFlag.FULL_MOB_BLOCK == CollisionFlag.FULL_MOB_BLOCK)
        assertEquals(0, flags and CollisionFlag.FULL_PROJECTILE_BLOCK)
    }

    @Test
    fun `break route finding marks route blocker for primary shapes`() {
        val manager = CollisionManager()
        val service = CollisionBuildService(manager)

        service.applyObject(
            id = 3001,
            x = 3210,
            y = 3210,
            z = 0,
            type = 10,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
            blockWalk = 2,
            blockRange = true,
            breakRouteFinding = true,
        )

        val flags = manager.getFlags(3210, 3210, 0)
        assertTrue(flags and CollisionFlag.ROUTE_BLOCKER != 0)
    }

    @Test
    fun `ground decor only blocks when blockWalk equals one`() {
        val manager = CollisionManager()
        val service = CollisionBuildService(manager)

        service.applyObject(
            id = 3002,
            x = 3220,
            y = 3220,
            z = 0,
            type = 22,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
            blockWalk = 2,
            blockRange = true,
            breakRouteFinding = false,
        )
        assertTrue(manager.canMove(3219, 3220, 3220, 3220, 0, 1, 1))

        service.applyObject(
            id = 3003,
            x = 3221,
            y = 3220,
            z = 0,
            type = 22,
            rotation = 0,
            sizeX = 1,
            sizeY = 1,
            solid = true,
            walkable = false,
            blockWalk = 1,
            blockRange = true,
            breakRouteFinding = false,
        )
        assertFalse(manager.canMove(3220, 3220, 3221, 3220, 0, 1, 1))
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
