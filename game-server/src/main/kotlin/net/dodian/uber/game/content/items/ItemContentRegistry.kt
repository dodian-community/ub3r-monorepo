package net.dodian.uber.game.content.items

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object ItemContentRegistry {
    private val logger = LoggerFactory.getLogger(ItemContentRegistry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byItemId = ConcurrentHashMap<Int, ItemContent>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return

        register(net.dodian.uber.game.content.items.runecrafting.RunePouchItems)
        register(net.dodian.uber.game.content.items.slayer.SlayerGemItems)
        register(net.dodian.uber.game.content.items.slayer.SlayerMaskItems)
        register(net.dodian.uber.game.content.items.consumables.DrinkItems)
        register(net.dodian.uber.game.content.items.consumables.FoodItems)
        register(net.dodian.uber.game.content.items.herblore.GrimyHerbItems)
        register(net.dodian.uber.game.content.items.herblore.HerbloreSuppliesItems)
        register(net.dodian.uber.game.content.items.utility.GuideBookItems)
        register(net.dodian.uber.game.content.items.cosmetics.ToyItems)
        register(net.dodian.uber.game.content.items.equipment.RepairHintItems)
        register(net.dodian.uber.game.content.items.events.EventInfoItems)
        register(net.dodian.uber.game.content.items.events.EventPackageItems)
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
