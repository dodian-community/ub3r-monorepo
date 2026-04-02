package net.dodian.uber.game.systems.content.items

import net.dodian.uber.game.content.ContentModuleIndex
import net.dodian.uber.game.content.items.ItemContent
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object ItemContentRegistry {
    private val logger = LoggerFactory.getLogger(ItemContentRegistry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byItemId = ConcurrentHashMap<Int, ItemContent>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return
        ContentModuleIndex.itemContents.forEach(::register)
    }

    fun register(content: ItemContent) {
        for (itemId in content.itemIds) {
            val existing = byItemId.putIfAbsent(itemId, content)
            if (existing != null) {
                logger.error(
                    "Duplicate ItemContent for itemId={} (existing={}, new={})",
                    itemId,
                    existing::class.java.name,
                    content::class.java.name
                )
            }
        }
    }

    fun get(itemId: Int): ItemContent? {
        ensureLoaded()
        return byItemId[itemId]
    }
}
