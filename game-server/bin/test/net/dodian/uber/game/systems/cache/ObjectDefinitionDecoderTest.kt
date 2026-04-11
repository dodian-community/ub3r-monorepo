package net.dodian.uber.game.systems.cache

import java.nio.file.Files
import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.systems.pathing.collision.CollisionManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObjectDefinitionDecoderTest {
    @AfterEach
    fun tearDown() {
        GameObjectData.replaceDefinitions(emptyMap())
        CollisionManager.global().clear()
    }

    @Test
    fun `cache bootstrap loads cache-backed object definitions and uses them for collision`() {
        val cacheDir = Files.createTempDirectory("cache-object-defs")
        CacheStoreTestFixtures.writeObjectDefinitionArchive(
            cacheDir,
            listOf(
                CacheStoreTestFixtures.FixtureObjectDefinition(
                    id = 1,
                    name = "Stone wall",
                    description = "fixture",
                    sizeX = 2,
                    sizeY = 1,
                    solid = true,
                    interactive = true,
                    interactions = listOf("Open"),
                    supportItems = 1,
                ),
            ),
        )
        CacheStoreTestFixtures.writeRegionArchives(
            root = cacheDir,
            regionId = 42,
            landscapeArchiveId = 43,
            objectArchiveId = 44,
            landscapeArchive = CacheStoreTestFixtures.createLandscapeArchive(),
            objectArchive =
                CacheStoreTestFixtures.createObjectArchive(
                    listOf(
                        CacheStoreTestFixtures.FixtureMapObject(
                            id = 1,
                            x = 1,
                            y = 2,
                            plane = 0,
                            type = 10,
                            rotation = 0,
                        ),
                    ),
                ),
        )

        val table = CacheBootstrapService(cacheDir).bootstrap()
        val definition = GameObjectData.forId(1)

        assertEquals("Stone wall", definition.name)
        assertEquals("fixture", definition.description)
        assertEquals(2, definition.sizeX)
        assertEquals(1, definition.sizeY)
        assertTrue(definition.isSolid())
        assertFalse(definition.isWalkable())
        assertTrue(definition.hasActions())
        assertEquals(1, table.summary.objectCount)
        assertEquals(1, table.summary.blockingObjectCount)
        assertFalse(CollisionManager.global().canMove(1, 2689, 1, 2690, 0, 1, 1))
    }

    @Test
    fun `hollow cache object definition clears solidity and does not block footprint`() {
        val cacheDir = Files.createTempDirectory("cache-object-defs-hollow")
        CacheStoreTestFixtures.writeObjectDefinitionArchive(
            cacheDir,
            listOf(
                CacheStoreTestFixtures.FixtureObjectDefinition(
                    id = 2,
                    name = "Ghost arch",
                    solid = true,
                    hollow = true,
                ),
            ),
        )
        CacheStoreTestFixtures.writeRegionArchives(
            root = cacheDir,
            regionId = 42,
            landscapeArchiveId = 43,
            objectArchiveId = 44,
            landscapeArchive = CacheStoreTestFixtures.createLandscapeArchive(),
            objectArchive =
                CacheStoreTestFixtures.createObjectArchive(
                    listOf(
                        CacheStoreTestFixtures.FixtureMapObject(
                            id = 2,
                            x = 4,
                            y = 4,
                            plane = 0,
                            type = 10,
                            rotation = 0,
                        ),
                    ),
                ),
        )

        CacheBootstrapService(cacheDir).bootstrap()
        val definition = GameObjectData.forId(2)

        assertFalse(definition.isSolid())
        assertTrue(definition.isWalkable())
        assertTrue(CollisionManager.global().canMove(4, 2691, 4, 2692, 0, 1, 1))
    }

    @Test
    fun `decoder tolerates cache variant opcode 27`() {
        val cacheDir = Files.createTempDirectory("cache-object-defs-op27")
        CacheStoreTestFixtures.writeObjectDefinitionArchive(
            cacheDir,
            listOf(
                CacheStoreTestFixtures.FixtureObjectDefinition(
                    id = 65,
                    name = "Compatibility object",
                    rawOpcodes = byteArrayOf(27),
                ),
            ),
        )

        val result = ObjectDefinitionDecoder.decode(CacheStore(cacheDir).open())

        assertEquals(66, result.definitionCount)
        assertEquals("Compatibility object", result.definitions[65]?.name)
    }

    @Test
    fun `decoder infers interactive object from model data when opcode 19 is absent`() {
        val cacheDir = Files.createTempDirectory("cache-object-defs-model-interactive")
        CacheStoreTestFixtures.writeObjectDefinitionArchive(
            cacheDir,
            listOf(
                CacheStoreTestFixtures.FixtureObjectDefinition(
                    id = 3,
                    name = "Model-driven object",
                    rawOpcodes = byteArrayOf(1, 1, 0, 1, 10),
                ),
            ),
        )

        val result = ObjectDefinitionDecoder.decode(CacheStore(cacheDir).open())

        assertTrue(result.definitions.getValue(3).hasActions())
    }

    @Test
    fun `non solid roofing object from cache remains traversable after collision rebuild`() {
        val cacheDir = Files.createTempDirectory("cache-object-defs-roofing-nonsolid")
        CacheStoreTestFixtures.writeObjectDefinitionArchive(
            cacheDir,
            listOf(
                CacheStoreTestFixtures.FixtureObjectDefinition(
                    id = 4,
                    name = "Roof deco",
                    solid = false,
                ),
            ),
        )
        CacheStoreTestFixtures.writeRegionArchives(
            root = cacheDir,
            regionId = 42,
            landscapeArchiveId = 43,
            objectArchiveId = 44,
            landscapeArchive = CacheStoreTestFixtures.createLandscapeArchive(),
            objectArchive =
                CacheStoreTestFixtures.createObjectArchive(
                    listOf(
                        CacheStoreTestFixtures.FixtureMapObject(
                            id = 4,
                            x = 6,
                            y = 6,
                            plane = 0,
                            type = 12,
                            rotation = 0,
                        ),
                    ),
                ),
        )

        CacheBootstrapService(cacheDir).bootstrap()
        val definition = GameObjectData.forId(4)

        assertFalse(definition.isSolid())
        assertTrue(definition.isWalkable())
        assertTrue(CollisionManager.global().canMove(5, 2694, 6, 2694, 0, 1, 1))
    }
}
