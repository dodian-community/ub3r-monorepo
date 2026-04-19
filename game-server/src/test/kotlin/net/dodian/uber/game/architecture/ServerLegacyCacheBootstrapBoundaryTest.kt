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
        val engineBootstrapPath = Paths.get("src/main/kotlin/net/dodian/uber/game/engine/lifecycle/EnginePluginBootstrap.kt")
        val source = Files.readString(serverPath)
        val engineBootstrapSource = Files.readString(engineBootstrapPath)
        assertFalse(source.contains("Cache.load()"))
        assertFalse(source.contains("ObjectDef.loadConfig()"))
        assertFalse(source.contains("new ObjectLoader()"))
        assertTrue(source.contains("new CacheBootstrapService().bootstrap()"))
        assertTrue(source.contains("ObjectClipService.bootstrapStartupOverlays(objects);"))
        assertTrue(source.contains("EnginePluginBootstrap.bootstrap();"))
        assertFalse(source.contains("launchWebApi();"))
        assertFalse(source.contains("WebApiKt"))
        assertFalse(source.contains("for (ContentBootstrap bootstrap : ContentModuleIndex.contentBootstraps)"))
        assertFalse(source.contains("GameEventBus.bootstrap();"))
        assertFalse(source.contains("ObjectContentRegistry.prewarmObjectDefinitions();"))
        assertTrue(engineBootstrapSource.contains("WebApi.start()"))

        val cacheBootstrapIndex = source.indexOf("new CacheBootstrapService().bootstrap()")
        val loadObjectsIndex = source.indexOf("loadObjects();")
        val doorRegistryIndex = source.indexOf("new DoorRegistry();")
        val overlayBootstrapIndex = source.indexOf("ObjectClipService.bootstrapStartupOverlays(objects);")
        val pluginBootstrapIndex = source.indexOf("EnginePluginBootstrap.bootstrap();")
        val nettyStartIndex = source.indexOf("nettyServer.start();")

        assertTrue(cacheBootstrapIndex in 0 until loadObjectsIndex)
        assertTrue(loadObjectsIndex in 0 until doorRegistryIndex)
        assertTrue(doorRegistryIndex in 0 until overlayBootstrapIndex)
        assertTrue(overlayBootstrapIndex in 0 until pluginBootstrapIndex)
        assertTrue(pluginBootstrapIndex in 0 until nettyStartIndex)
    }
}
