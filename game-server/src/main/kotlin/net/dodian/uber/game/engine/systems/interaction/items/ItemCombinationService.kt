package net.dodian.uber.game.engine.systems.interaction.items

import net.dodian.uber.game.item.admin.PotatoItemInteractionState
import net.dodian.uber.game.item.cosmetic.NoveltyItemCombinations
import net.dodian.uber.game.item.equipment.RepairPlaceholderItemCombinations
import net.dodian.uber.game.engine.systems.skills.farming.SaplingItemCombinations
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.api.content.ContentActionCancelReason
import net.dodian.uber.game.api.content.ContentActions
import net.dodian.uber.game.engine.systems.skills.SkillInteractionDispatcher
import net.dodian.uber.game.engine.systems.quests.QuestInteractionDispatcher

object ItemCombinationService {
    @JvmStatic
    fun handle(client: Client, usedWithSlot: Int, itemUsedSlot: Int) {
        val lastSlot = client.playerItems.size - 1
        if (usedWithSlot !in 0..lastSlot || itemUsedSlot !in 0..lastSlot) {
            // Malformed slot = drop the packet, never disconnect (see PacketItemActionService).
            return
        }

        val useWith = client.playerItems[usedWithSlot] - 1
        val itemUsed = client.playerItems[itemUsedSlot] - 1
        if (!client.playerHasItem(itemUsed) || !client.playerHasItem(useWith)) {
            return
        }

        ContentActions.cancel(
            client,
            ContentActionCancelReason.ITEM_INTERACTION,
            false,
            false,
            false,
            true,
        )

        if (SkillInteractionDispatcher.tryHandleItemOnItem(client, itemUsed, useWith)) {
            return
        }

        if (QuestInteractionDispatcher.tryHandleItemOnItem(client, itemUsed, useWith)) {
            return
        }

        if (useWith == 5733 || itemUsed == 5733) {
            PotatoItemInteractionState.beginItemOnItem(
                client,
                if (useWith == 5733) itemUsedSlot else usedWithSlot,
                if (useWith == 5733) itemUsed else useWith,
            )
            return
        }

        SaplingItemCombinations.handle(client, useWith, usedWithSlot, itemUsed, itemUsedSlot)

        val otherItem = client.playerItems[usedWithSlot] - 1
        val knife = (useWith == 946 || itemUsed == 946) || (useWith == 5605 || itemUsed == 5605)

        if (NoveltyItemCombinations.handle(client, itemUsed, otherItem, itemUsedSlot, usedWithSlot, knife)) {
            return
        }

        RepairPlaceholderItemCombinations.handle(client, itemUsed, useWith)
    }
}
