package net.dodian.uber.game.engine.systems.interaction.items

import net.dodian.uber.game.api.content.ContentErrorPolicy
import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.item.ItemClickEvent
import net.dodian.uber.game.events.item.ItemOptionClickEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.skills.SkillInteractionDispatcher

object ItemDispatcher {
    @JvmStatic
    fun tryHandle(client: Client, option: Int, itemId: Int, itemSlot: Int, interfaceId: Int): Boolean {
        val eventHandled =
            if (option == 1) {
                GameEventBus.postWithResult(ItemClickEvent(client, itemId, itemSlot, interfaceId))
            } else {
                GameEventBus.postWithResult(ItemOptionClickEvent(client, option, itemId, itemSlot, interfaceId))
            }
        if (eventHandled) {
            return true
        }

        if (SkillInteractionDispatcher.tryHandleItemClick(client, option, itemId, itemSlot, interfaceId)) {
            return true
        }
        val content = ItemContentRegistry.get(itemId) ?: return false
        return ContentErrorPolicy.runBoolean(client, "item.dispatch.option.$option") {
            when (option) {
                1 -> content.onFirstClick(client, itemId, itemSlot, interfaceId)
                2 -> content.onSecondClick(client, itemId, itemSlot, interfaceId)
                3 -> content.onThirdClick(client, itemId, itemSlot, interfaceId)
                else -> false
            }
        }
    }
}
