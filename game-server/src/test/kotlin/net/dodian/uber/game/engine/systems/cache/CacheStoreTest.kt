package net.dodian.uber.game.engine.systems.cache

import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class CacheStoreTest {
    @Test
    fun `reads multi-sector file payload from indexed store`() {
        val cacheDir = Files.createTempDirectory("cache-store-sectors")
        val payload = ByteArray(700) { index -> (index and 0xFF).toByte() }
        CacheStoreTestFixtures.writeStoreFile(cacheDir, cache = 4, file = 321, payload = payload)

        val store = CacheStore(cacheDir).open()
        val decoded = store.readStoreFile(cache = 4, file = 321)

        assertNotNull(decoded)
        assertArrayEquals(payload, decoded)
    }

    @Test
    fun `reads map index from archive zero file five`() {
        val cacheDir = Files.createTempDirectory("cache-store-versionlist")
        val mapIndex = CacheStoreTestFixtures.createMapIndex(regionId = 42, landscapeArchiveId = 43, objectArchiveId = 44)
        CacheStoreTestFixtures.writeVersionListArchive(cacheDir, mapIndex)

        val store = CacheStore(cacheDir).open()
        val decoded = store.readMapIndex()

        assertNotNull(decoded)
        assertArrayEquals(mapIndex, decoded)
    }
}

