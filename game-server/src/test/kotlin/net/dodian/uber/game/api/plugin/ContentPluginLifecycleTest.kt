package net.dodian.uber.game.api.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContentPluginLifecycleTest {
    @org.junit.jupiter.api.Test
    fun `generic social descriptors are discovered and route ownership is unique`() {
        val ids = ContentModuleIndex.socialPlugins.map { it.contentManifest.id }.toSet()
        org.junit.jupiter.api.Assertions.assertEquals(setOf("social.trading", "social.dueling"), ids)
        val routes = ContentModuleIndex.socialPlugins.flatMap { it.contentManifest.declaredRouteKeys }
        org.junit.jupiter.api.Assertions.assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `plugins start once in manifest order and stop in reverse order`() {
        val events = mutableListOf<String>()
        fun plugin(id: String) = object : ContentPlugin {
            override val pluginMetadata = PluginModuleMetadata(id, id, owner = "test")
            override val contentManifest = ContentModuleManifest(id, "test", "1.0.0")
            override fun start() { events += "start:$id" }
            override fun stop() { events += "stop:$id" }
        }
        val z = plugin("test.z")
        val a = plugin("test.a")
        ContentPluginLifecycle.resetForTests()
        ContentPluginLifecycle.start(listOf(z, a, z))
        ContentPluginLifecycle.stop()
        assertEquals(listOf("start:test.a", "start:test.z", "stop:test.z", "stop:test.a"), events)
    }

    @Test
    fun `owned resources close before plugin stop`() {
        val events = mutableListOf<String>()
        val plugin = object : ContentPlugin {
            override val pluginMetadata = PluginModuleMetadata("test.resources", "test", owner = "test")
            override val contentManifest = ContentModuleManifest("test.resources", "test", "1.0.0")
            override fun start() { ContentPluginLifecycle.own(this, AutoCloseable { events += "close" }) }
            override fun stop() { events += "stop" }
        }
        ContentPluginLifecycle.resetForTests()
        ContentPluginLifecycle.start(listOf(plugin))
        ContentPluginLifecycle.stop()
        assertEquals(listOf("close", "stop"), events)
    }
}
