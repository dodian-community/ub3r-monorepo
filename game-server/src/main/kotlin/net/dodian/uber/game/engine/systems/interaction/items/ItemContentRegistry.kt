package net.dodian.uber.game.engine.systems.interaction.items

import net.dodian.uber.game.api.plugin.ContentModuleIndex
import net.dodian.uber.game.content.items.ItemContent
import net.dodian.uber.game.api.plugin.ContentBootstrap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object ItemContentRegistry : ContentBootstrap {
    override val id: String = "items.registry"

    private val loaded = AtomicBoolean(false)
    private val byItemId = ConcurrentHashMap<Int, ItemContent>()

    override fun bootstrap() {
        if (!loaded.compareAndSet(false, true)) return
        ContentModuleIndex.itemContents.forEach(::register)
    }

    fun register(content: ItemContent) {
        for (itemId in content.itemIds) {
            val existing = byItemId.putIfAbsent(itemId, content)
            check(existing == null) {
                "Duplicate ItemContent for itemId=$itemId existing=${existing?.javaClass?.name} new=${content::class.java.name}"
            }
        }
    }

    fun get(itemId: Int): ItemContent? {
        bootstrap()
        return byItemId[itemId]
    }
}
