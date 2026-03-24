package net.dodian.uber.game.skills.core

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.crafting.GoldJewelryRequest
import net.dodian.uber.game.skills.crafting.api.CraftingPlugin
import net.dodian.uber.game.skills.smithing.api.SmithingPlugin
import net.dodian.uber.game.skills.smithing.SmeltingInterfaceService
import net.dodian.uber.game.skills.smithing.SmithingInterfaceService

object SkillingInterfaceItemService {
    @JvmStatic
    fun handleContainerAmount(
        client: Client,
        interfaceId: Int,
        itemId: Int,
        slot: Int,
        amount: Int,
    ): Boolean {
        return when {
            SmeltingInterfaceService.isSmeltingInterfaceFrame(interfaceId) -> {
                SmithingPlugin.startSmeltingFromItem(client, itemId, amount)
                true
            }
            interfaceId in 1119..1123 -> {
                SmithingPlugin.startSmithing(client, itemId, amount)
                true
            }
            interfaceId in 4233..4257 -> {
                CraftingPlugin.startGoldJewelry(client, GoldJewelryRequest(interfaceId, slot, amount))
                true
            }
            else -> false
        }
    }
}
