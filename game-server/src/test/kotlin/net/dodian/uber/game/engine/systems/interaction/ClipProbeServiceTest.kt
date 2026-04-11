package net.dodian.uber.game.engine.systems.interaction

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.engine.systems.cache.CacheCollisionAuditStore
import net.dodian.uber.game.engine.systems.cache.DecodedMapObject
import net.dodian.uber.game.engine.systems.cache.MapIndexEntry
import net.dodian.uber.game.engine.systems.pathing.collision.CollisionManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClipProbeServiceTest {
    @Test
    fun `clip probe flags footprint mismatch when only rotated footprint overlaps`() {
        val collision = CollisionManager.global()
        collision.clear()
        CacheCollisionAuditStore.clearForTests()
        ObjectClipService.clearForTests()

        try {
            GameObjectData.addDefinition(
                GameObjectData(
                    id = 4000,
                    name = "probe obj",
                    description = "fixture",
                    sizeX = 2,
                    sizeY = 1,
                    solid = true,
                    walkable = false,
                    hasActionsFlag = true,
                    unknownValue = false,
                    walkType = 0,
                ),
            )
            CacheCollisionAuditStore.publish(
                regions = listOf(MapIndexEntry(regionId = CacheCollisionAuditStore.regionId(2727, 9773), landscapeArchiveId = -1, objectArchiveId = -1)),
                regionObjects =
                    mapOf(
                        CacheCollisionAuditStore.regionId(2727, 9773) to
                            listOf(
                                DecodedMapObject(
                                    objectId = 4000,
                                    x = 2727,
                                    y = 9772,
                                    plane = 0,
                                    type = 10,
                                    rotation = 1,
                                    regionId = CacheCollisionAuditStore.regionId(2727, 9773),
                                ),
                            ),
                    ),
            )

            val probe = ClipProbeService.probeTile(2727, 9773, 0)
            assertTrue(probe.blockedByCurrentObjects)
            assertTrue(!probe.blockedByLunaObjects)
            assertTrue(probe.likelyFootprintMismatch)
            assertEquals(1, probe.objectMatches.size)
        } finally {
            CacheCollisionAuditStore.clearForTests()
            ObjectClipService.clearForTests()
            collision.clear()
        }
    }

    @Test
    fun `clip probe overlap summary includes object fields and reason tags`() {
        val collision = CollisionManager.global()
        collision.clear()
        CacheCollisionAuditStore.clearForTests()
        ObjectClipService.clearForTests()

        try {
            GameObjectData.addDefinition(
                GameObjectData(
                    id = 4001,
                    name = "wall",
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
            val regionId = CacheCollisionAuditStore.regionId(3200, 3200)
            CacheCollisionAuditStore.publish(
                regions = listOf(MapIndexEntry(regionId = regionId, landscapeArchiveId = -1, objectArchiveId = -1)),
                regionObjects =
                    mapOf(
                        regionId to
                            listOf(
                                DecodedMapObject(
                                    objectId = 4001,
                                    x = 3200,
                                    y = 3200,
                                    plane = 0,
                                    type = 0,
                                    rotation = 0,
                                    regionId = regionId,
                                ),
                            ),
                    ),
            )

            val probe = ClipProbeService.probeTile(3201, 3200, 0)
            val summary = ClipProbeService.formatOverlapSummary(probe, 3)
            assertFalse(summary == "none")
            assertTrue(summary.contains("id=4001"))
            assertTrue(summary.contains("name=wall"))
            assertTrue(summary.contains("type=0"))
            assertTrue(summary.contains("reasons=blocked_by_type_rule+directional_block"))
        } finally {
            CacheCollisionAuditStore.clearForTests()
            ObjectClipService.clearForTests()
            collision.clear()
        }
    }

    @Test
    fun `type 4 overlap is visible but not blocked by type rule`() {
        val collision = CollisionManager.global()
        collision.clear()
        CacheCollisionAuditStore.clearForTests()
        ObjectClipService.clearForTests()

        try {
            GameObjectData.addDefinition(
                GameObjectData(
                    id = 4002,
                    name = "decoration",
                    description = "fixture",
                    sizeX = 1,
                    sizeY = 1,
                    solid = true,
                    walkable = false,
                    hasActionsFlag = false,
                    unknownValue = false,
                    walkType = 0,
                ),
            )
            val regionId = CacheCollisionAuditStore.regionId(3300, 3300)
            CacheCollisionAuditStore.publish(
                regions = listOf(MapIndexEntry(regionId = regionId, landscapeArchiveId = -1, objectArchiveId = -1)),
                regionObjects =
                    mapOf(
                        regionId to
                            listOf(
                                DecodedMapObject(
                                    objectId = 4002,
                                    x = 3300,
                                    y = 3300,
                                    plane = 0,
                                    type = 4,
                                    rotation = 0,
                                    regionId = regionId,
                                ),
                            ),
                    ),
            )

            val probe = ClipProbeService.probeTile(3300, 3300, 0)
            assertEquals(1, probe.objectMatches.size)
            val overlap = probe.objectMatches.first()
            assertFalse(overlap.blocksByTypeRule)
            assertFalse(overlap.reasonTags.contains("blocked_by_type_rule"))
        } finally {
            CacheCollisionAuditStore.clearForTests()
            ObjectClipService.clearForTests()
            collision.clear()
        }
    }

    @Test
    fun `type 10 solid non-interactive overlap is visible and blocked by type rule`() {
        val collision = CollisionManager.global()
        collision.clear()
        CacheCollisionAuditStore.clearForTests()
        ObjectClipService.clearForTests()

        try {
            GameObjectData.addDefinition(
                GameObjectData(
                    id = 4003,
                    name = "default deco",
                    description = "fixture",
                    sizeX = 1,
                    sizeY = 1,
                    solid = true,
                    walkable = false,
                    hasActionsFlag = false,
                    unknownValue = false,
                    walkType = 0,
                ),
            )
            val regionId = CacheCollisionAuditStore.regionId(3400, 3400)
            CacheCollisionAuditStore.publish(
                regions = listOf(MapIndexEntry(regionId = regionId, landscapeArchiveId = -1, objectArchiveId = -1)),
                regionObjects =
                    mapOf(
                        regionId to
                            listOf(
                                DecodedMapObject(
                                    objectId = 4003,
                                    x = 3400,
                                    y = 3400,
                                    plane = 0,
                                    type = 10,
                                    rotation = 0,
                                    regionId = regionId,
                                ),
                            ),
                    ),
            )

            val probe = ClipProbeService.probeTile(3400, 3400, 0)
            assertEquals(1, probe.objectMatches.size)
            val overlap = probe.objectMatches.first()
            assertTrue(overlap.blocksByTypeRule)
            assertTrue(overlap.reasonTags.contains("blocked_by_type_rule"))
        } finally {
            CacheCollisionAuditStore.clearForTests()
            ObjectClipService.clearForTests()
            collision.clear()
        }
    }
}
