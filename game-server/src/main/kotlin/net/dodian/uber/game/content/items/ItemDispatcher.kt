package net.dodian.uber.game.content.items

import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

object ItemDispatcher {
    private val logger = LoggerFactory.getLogger(ItemDispatcher::class.java)

    @JvmStatic
    fun tryHandle(client: Client, option: Int, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        val content = ItemContentRegistry.get(itemId) ?: return false
        return try {
            when (option) {
                1 -> content.onFirstClick(client, itemId, itemSlot, interfaceId)
                2 -> content.onSecondClick(client, itemId, itemSlot, interfaceId)
                3 -> content.onThirdClick(client, itemId, itemSlot, interfaceId)
                else -> false
            }
        } catch (e: Exception) {
            logger.error(
                "Error handling item click (option={}, itemId={}, slot={}) via {}",
                option,
                itemId,
                itemSlot,
                content::class.java.name,
                e
            )
            false
        }
    }
}

