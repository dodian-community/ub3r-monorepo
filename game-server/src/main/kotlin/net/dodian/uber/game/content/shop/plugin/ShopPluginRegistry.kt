package net.dodian.uber.game.content.shop.plugin

import net.dodian.uber.game.content.shop.ShopDefinition
import net.dodian.uber.game.systems.dispatch.ContentBootstrap
import net.dodian.uber.game.systems.dispatch.ContentModuleIndex
import java.util.concurrent.atomic.AtomicBoolean

object ShopPluginRegistry : ContentBootstrap {
    override val id: String = "shops.registry"
    private val bootstrapped = AtomicBoolean(false)
    private val registeredPlugins = mutableListOf<ShopPlugin>()
    @Volatile
    private var snapshot: Map<Int, ShopDefinition> = emptyMap()

    override fun bootstrap() {
        if (bootstrapped.get()) return
        synchronized(this) {
            if (bootstrapped.get()) return
            val definitions =
                if (registeredPlugins.isNotEmpty()) {
                    registeredPlugins.map { it.definition }
                } else {
                    ContentModuleIndex.shopPlugins.map { it.definition }
                }
            snapshot = buildSnapshot(definitions)
            bootstrapped.set(true)
        }
    }

    fun register(plugin: ShopPlugin) {
        synchronized(this) {
            registeredPlugins += plugin
            if (bootstrapped.get()) {
                snapshot = buildSnapshot(
                    if (registeredPlugins.isNotEmpty()) {
                        registeredPlugins.map { it.definition }
                    } else {
                        ContentModuleIndex.shopPlugins.map { it.definition }
                    },
                )
            }
        }
    }

    fun all(): List<ShopDefinition> {
        bootstrap()
        return snapshot.values.sortedBy { it.id }
    }

    fun find(id: Int): ShopDefinition? {
        bootstrap()
        return snapshot[id]
    }

    fun resetForTests() {
        synchronized(this) {
            registeredPlugins.clear()
            snapshot = emptyMap()
            bootstrapped.set(false)
        }
    }

    private fun buildSnapshot(definitions: List<ShopDefinition>): Map<Int, ShopDefinition> {
        val byId = linkedMapOf<Int, ShopDefinition>()
        definitions.forEach { definition ->
            val existing = byId.putIfAbsent(definition.id, definition)
            require(existing == null) {
                "Duplicate shop definition id=${definition.id}"
            }
        }
        return byId
    }
}
