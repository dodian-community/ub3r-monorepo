package net.dodian.uber.game.content.interfaces.skilling

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.crafting.GoldJewelryRequest
import net.dodian.uber.game.content.skills.crafting.CraftingPlugin
import net.dodian.uber.game.content.skills.smithing.SmithingPlugin
import net.dodian.uber.game.content.skills.smithing.SmeltingInterfaceService

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
