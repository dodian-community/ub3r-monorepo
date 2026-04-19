package net.dodian.uber.game.engine.systems.cache

import net.dodian.cache.objects.GameObjectData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class MapDecoderSmokeTest {
    @Test
    fun `decoder reads map index and decodes region tiles and objects`() {
        val cacheDir = Files.createTempDirectory("cache-store-smoke")
        GameObjectData.addDefinition(
            GameObjectData(
                id = 1000,
                name = "Test blocking object",
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

        val landscapeArchive =
            CacheStoreTestFixtures.createLandscapeArchive(
                mapOf(
                    CacheStoreTestFixtures.TileCoordinate(x = 1, y = 2, plane = 0) to byteArrayOf(50, 82.toByte(), 0),
                ),
            )
        val objectArchive =
            CacheStoreTestFixtures.createObjectArchive(
                listOf(
                    CacheStoreTestFixtures.FixtureMapObject(
                        id = 1000,
                        x = 1,
                        y = 2,
                        plane = 0,
                        type = 10,
                        rotation = 3,
                    ),
                ),
            )
        CacheStoreTestFixtures.writeRegionArchives(
            root = cacheDir,
            regionId = 42,
            landscapeArchiveId = 43,
            objectArchiveId = 44,
            landscapeArchive = landscapeArchive,
            objectArchive = objectArchive,
            priority = true,
        )

        val store = CacheStore(cacheDir).open()
        val table = MapDecoder(store).decodeIndexTable()

        assertTrue(table.regions.isNotEmpty())
        assertEquals(1, table.regions.size)
        assertEquals(42, table.regions.first().regionId)
        assertTrue(table.regions.first().priority)

        val tileGrid = table.tileGrids[42]
        assertNotNull(tileGrid)

        val decodedTile = tileGrid!!.getTile(1, 2, 0)
        assertTrue(decodedTile.isBlocked())
        assertFalse(decodedTile.isBridge())
        assertEquals(1, decodedTile.underlay)

        assertEquals(1, table.objects.size)
        val decodedObject = table.objects.first()
        assertEquals(1000, decodedObject.objectId)
        assertEquals(1, decodedObject.x)
        assertEquals(2690, decodedObject.y)
        assertEquals(0, decodedObject.plane)
        assertEquals(10, decodedObject.type)
        assertEquals(3, decodedObject.rotation)

        assertEquals(1, table.summary.regionCount)
        assertEquals(64 * 64 * 4, table.summary.tileCount)
        assertEquals(1, table.summary.objectCount)
        assertEquals(1, table.summary.blockingObjectCount)
        assertEquals(0, table.summary.walkableObjectCount)
    }
}
