package net.dodian.uber.game.engine.systems.interaction.items

import net.dodian.uber.game.item.admin.PotatoItemInteractionState
import net.dodian.uber.game.item.cosmetic.NoveltyItemCombinations
import net.dodian.uber.game.item.equipment.RepairPlaceholderItemCombinations
import net.dodian.uber.game.skill.farming.SaplingItemCombinations
import net.dodian.uber.game.skill.firemaking.FiremakingItemCombinations
import net.dodian.uber.game.skill.fletching.FletchingItemCombinations
import net.dodian.uber.game.skill.herblore.HerbloreItemCombinations
import net.dodian.uber.game.skill.crafting.CraftingItemCombinations
import net.dodian.uber.game.skill.slayer.EquipmentAssemblyItemCombinations
import net.dodian.uber.game.skill.smithing.rockshell.DialogueGateItemCombinations
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.api.content.ContentActionCancelReason
import net.dodian.uber.game.api.content.ContentActions
import net.dodian.uber.game.engine.systems.skills.SkillInteractionDispatcher

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

        if (CraftingItemCombinations.handleCrystalKey(client, itemUsed, otherItem, itemUsedSlot, usedWithSlot)) {
            return
        }

        if (DialogueGateItemCombinations.handle(client, itemUsed, useWith)) {
            return
        }

        if (HerbloreItemCombinations.handle(client, itemUsed, otherItem)) {
            return
        }

        if (HerbloreItemCombinations.handleDoseMixing(client, itemUsed, useWith)) {
            return
        }

        if (NoveltyItemCombinations.handle(client, itemUsed, otherItem, itemUsedSlot, usedWithSlot, knife)) {
            return
        }

        if (EquipmentAssemblyItemCombinations.handle(client, itemUsed, otherItem)) {
            return
        }

        if (FletchingItemCombinations.handle(client, itemUsed, otherItem, knife)) {
            return
        }

        if (CraftingItemCombinations.handle(client, itemUsed, otherItem, itemUsedSlot, usedWithSlot)) {
            return
        }

        if (FiremakingItemCombinations.handle(client, itemUsed, useWith)) {
            return
        }

        RepairPlaceholderItemCombinations.handle(client, itemUsed, useWith)
    }
}
