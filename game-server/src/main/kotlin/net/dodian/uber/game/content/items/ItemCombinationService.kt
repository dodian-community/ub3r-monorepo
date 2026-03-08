package net.dodian.uber.game.content.items

import net.dodian.uber.game.content.items.combination.CraftingItemCombinationHandler
import net.dodian.uber.game.content.items.combination.DialogueGateItemCombinationHandler
import net.dodian.uber.game.content.items.combination.EquipmentAssemblyItemCombinationHandler
import net.dodian.uber.game.content.items.combination.FiremakingItemCombinationHandler
import net.dodian.uber.game.content.items.combination.FletchingItemCombinationHandler
import net.dodian.uber.game.content.items.combination.HerbloreItemCombinationHandler
import net.dodian.uber.game.content.items.combination.NoveltyItemCombinationHandler
import net.dodian.uber.game.content.items.combination.RepairPlaceholderItemCombinationHandler
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason

object ItemCombinationService {
    private const val MAX_INVENTORY_SLOT = 28

    @JvmStatic
    fun handle(client: Client, usedWithSlot: Int, itemUsedSlot: Int) {
        if (usedWithSlot !in 0..MAX_INVENTORY_SLOT || itemUsedSlot !in 0..MAX_INVENTORY_SLOT) {
            client.disconnected = true
            return
        }

        val useWith = client.playerItems[usedWithSlot] - 1
        val itemUsed = client.playerItems[itemUsedSlot] - 1
        if (!client.playerHasItem(itemUsed) || !client.playerHasItem(useWith)) {
            return
        }

        PlayerActionCancellationService.cancel(
            client,
            PlayerActionCancelReason.ITEM_INTERACTION,
            false,
            false,
            false,
            true,
        )

        if (useWith == 5733 || itemUsed == 5733) {
            PotatoInteractionState.beginItemOnItem(
                client,
                if (useWith == 5733) itemUsedSlot else usedWithSlot,
                if (useWith == 5733) itemUsed else useWith,
            )
            return
        }

        SaplingItemCombinationHandler.handle(client, useWith, usedWithSlot, itemUsed, itemUsedSlot)

        val otherItem = client.playerItems[usedWithSlot] - 1
        val knife = (useWith == 946 || itemUsed == 946) || (useWith == 5605 || itemUsed == 5605)

        if (CraftingItemCombinationHandler.handleCrystalKey(client, itemUsed, otherItem, itemUsedSlot, usedWithSlot)) {
            return
        }

        if (DialogueGateItemCombinationHandler.handle(client, itemUsed, useWith)) {
            return
        }

        if (HerbloreItemCombinationHandler.handle(client, itemUsed, otherItem)) {
            return
        }

        if (HerbloreItemCombinationHandler.handleDoseMixing(client, itemUsed, useWith)) {
            return
        }

        if (NoveltyItemCombinationHandler.handle(client, itemUsed, otherItem, itemUsedSlot, usedWithSlot, knife)) {
            return
        }

        if (EquipmentAssemblyItemCombinationHandler.handle(client, itemUsed, otherItem)) {
            return
        }

        if (FletchingItemCombinationHandler.handle(client, itemUsed, otherItem, knife)) {
            return
        }

        if (CraftingItemCombinationHandler.handle(client, itemUsed, otherItem, itemUsedSlot, usedWithSlot)) {
            return
        }

        if (FiremakingItemCombinationHandler.handle(client, itemUsed, useWith)) {
            return
        }

        RepairPlaceholderItemCombinationHandler.handle(client, itemUsed, useWith)
    }
}
