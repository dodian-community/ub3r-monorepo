package net.dodian.uber.game.engine.systems.interaction.items

import net.dodian.uber.game.engine.event.GameEventBus
import net.dodian.uber.game.events.item.ItemClickEvent
import net.dodian.uber.game.events.item.ItemOptionClickEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.skills.SkillInteractionDispatcher
import net.dodian.uber.game.engine.systems.quests.QuestInteractionDispatcher

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

        return QuestInteractionDispatcher.tryHandleItemClick(client, option, itemId, itemSlot, interfaceId)
    }
}