package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServerLegacyCacheBootstrapBoundaryTest {
    @Test
    fun `server bootstrap no longer calls removed cache loaders`() {
        val serverPath = Paths.get("src/main/java/net/dodian/uber/game/Server.java")
        val source = Files.readString(serverPath)
        assertFalse(source.contains("Cache.load()"))
        assertFalse(source.contains("ObjectDef.loadConfig()"))
        assertFalse(source.contains("new ObjectLoader()"))
        assertTrue(source.contains("new CacheBootstrapService().bootstrap()"))
        assertTrue(source.contains("ObjectClipService.bootstrapStartupOverlays(objects);"))

        val cacheBootstrapIndex = source.indexOf("new CacheBootstrapService().bootstrap()")
        val loadObjectsIndex = source.indexOf("loadObjects();")
        val doorRegistryIndex = source.indexOf("new DoorRegistry();")
        val overlayBootstrapIndex = source.indexOf("ObjectClipService.bootstrapStartupOverlays(objects);")
        val nettyStartIndex = source.indexOf("nettyServer.start();")

        assertTrue(cacheBootstrapIndex in 0 until loadObjectsIndex)
        assertTrue(loadObjectsIndex in 0 until doorRegistryIndex)
        assertTrue(doorRegistryIndex in 0 until overlayBootstrapIndex)
        assertTrue(overlayBootstrapIndex in 0 until nettyStartIndex)
    }
}
