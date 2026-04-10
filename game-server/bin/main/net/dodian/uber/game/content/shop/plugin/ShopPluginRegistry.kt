package net.dodian.uber.game.content.shop.plugin

import net.dodian.uber.game.content.shop.ShopDefinition
import net.dodian.uber.game.content.shop.ShopManager
import net.dodian.uber.game.systems.dispatch.ContentBootstrap
import net.dodian.uber.game.systems.dispatch.ContentModuleIndex
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

object ShopPluginRegistry : ContentBootstrap {
    private val logger = LoggerFactory.getLogger("ShopPluginRegistry")
    override val id: String = "shops.registry"
    private val bootstrapped = AtomicBoolean(false)
    private val registeredPlugins = mutableListOf<ShopPlugin>()
    @Volatile
    private var snapshot: Map<Int, ShopDefinition> = emptyMap()

    override fun bootstrap() {
        if (bootstrapped.get()) return
        synchronized(this) {
            if (bootstrapped.get()) return
            val loadedPlugins =
                if (registeredPlugins.isNotEmpty()) {
                    registeredPlugins.toList()
                } else {
                    ContentModuleIndex.shopPlugins
                }
            val definitions = loadedPlugins.map { it.definition }
            snapshot = buildSnapshot(definitions)
            val source = if (registeredPlugins.isNotEmpty()) "registered" else "generated"
            logger.info(
                "shop plugins bootstrapped {} plugins (shops={}, source={})",
                loadedPlugins.size,
                snapshot.size,
                source,
            )
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
            require(definition.id in 0 until ShopManager.MaxShops) {
                "Shop id out of range: ${definition.id}"
            }
            require(definition.stock.size <= ShopManager.MaxShopItems) {
                "Shop ${definition.id} exceeds max items"
            }
            require(definition.slotBuyPriceOverrides.keys.all { it in definition.stock.indices }) {
                "Shop ${definition.id} has slot override outside stock range"
            }
            val existing = byId.putIfAbsent(definition.id, definition)
            require(existing == null) {
                "Duplicate shop definition id=${definition.id}"
            }
        }
        return byId
    }
}
