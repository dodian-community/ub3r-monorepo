package net.dodian.uber.game.skills.crafting

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SetGoldItems
import net.dodian.uber.game.runtime.action.ProductionActionService
import net.dodian.uber.game.runtime.action.ProductionRequest

object GoldJewelryService {
    private const val GOLD_BAR_ID = 2357
    private const val GOLD_CRAFT_ANIMATION = 0x383

    @JvmStatic
    fun openInterface(client: Client) {
        var slot: Int
        for (index in 0 until GoldJewelryDefinitions.interfaceSlots.size - 1) {
            slot = GoldJewelryDefinitions.interfaceSlots[index]
            if (!client.playerHasItem(GoldJewelryDefinitions.requiredMoulds[index])) {
                client.changeInterfaceStatus(slot - 5, true)
                client.changeInterfaceStatus(slot - 1, false)
                continue
            } else {
                client.changeInterfaceStatus(slot - 5, false)
                client.changeInterfaceStatus(slot - 1, true)
            }
            val itemsToShow = IntArray(7)
            for (itemSlot in itemsToShow.indices) {
                itemsToShow[itemSlot] = resolveShownItem(client, index, itemSlot)
                if (itemSlot != 0 && itemsToShow[itemSlot] != GoldJewelryDefinitions.jewelryByGroup[index][itemSlot]) {
                    client.sendFrame246(slot + 13 + itemSlot - 1 - index, GoldJewelryDefinitions.frameSizes[index], GoldJewelryDefinitions.blackFrames[index])
                } else if (itemSlot != 0) {
                    client.sendFrame246(slot + 13 + itemSlot - 1 - index, GoldJewelryDefinitions.frameSizes[index], -1)
                }
            }
            client.send(SetGoldItems(slot, itemsToShow))
        }
        client.showInterface(4161)
    }

    @JvmStatic
    fun start(client: Client, request: GoldJewelryRequest): Boolean {
        val index = resolveIndex(request.interfaceId)
        val product = GoldJewelryDefinitions.product(index, request.slot) ?: return false
        if (product.requiredLevel > client.getLevel(Skill.CRAFTING)) {
            client.send(SendMessage("You need a crafting level of ${product.requiredLevel} to make this."))
            return true
        }
        if (!client.playerHasItem(GOLD_BAR_ID)) {
            client.send(SendMessage("You need at least one gold bar."))
            return true
        }
        val requiredGem = GoldJewelryDefinitions.requiredGemItems[request.slot]
        if (request.slot != 0 && !client.playerHasItem(requiredGem)) {
            client.send(SendMessage("You need a ${client.GetItemName(requiredGem).lowercase()} to make this."))
            return true
        }

        client.send(RemoveInterfaces())
        return ProductionActionService.start(
            client,
            ProductionRequest(
                skillId = Skill.CRAFTING.id,
                productId = product.productId,
                amountPerCycle = 1,
                primaryItemId = GOLD_BAR_ID,
                secondaryItemId = if (request.slot == 0) -1 else requiredGem,
                experiencePerUnit = product.experience,
                animationId = GOLD_CRAFT_ANIMATION,
                tickDelay = 3,
                completionMessage = "You craft a ${client.GetItemName(product.productId).lowercase()}",
            ),
            request.amount,
        )
    }

    @JvmStatic
    fun findStrungAmulet(amuletId: Int): Int {
        val product = GoldJewelryDefinitions.findProductByAmulet(amuletId) ?: return -1
        return GoldJewelryDefinitions.strungAmulets.getOrElse(product.slot) { -1 }
    }

    private fun resolveShownItem(client: Client, index: Int, slot: Int): Int {
        val requiredGem = GoldJewelryDefinitions.requiredGemItems[slot]
        if (!client.playerHasItem(requiredGem) && requiredGem != -1) {
            return GoldJewelryDefinitions.blanks[index][slot]
        }
        return GoldJewelryDefinitions.jewelryByGroup[index][slot]
    }

    private fun resolveIndex(interfaceId: Int): Int {
        var index = 0
        for (candidate in 0..2) {
            if (GoldJewelryDefinitions.interfaceSlots[candidate] == interfaceId) {
                index = candidate
            }
        }
        return index
    }
}
