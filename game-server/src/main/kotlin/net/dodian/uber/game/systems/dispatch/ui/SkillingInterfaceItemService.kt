package net.dodian.uber.game.systems.dispatch.ui

import net.dodian.uber.game.content.skills.crafting.Crafting
import net.dodian.uber.game.content.skills.crafting.GoldJewelryRequest
import net.dodian.uber.game.content.skills.smithing.SmithingInterface
import net.dodian.uber.game.content.skills.smithing.Smithing
import net.dodian.uber.game.model.entity.player.Client

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
            SmithingInterface.isSmeltingInterfaceFrame(interfaceId) -> {
                Smithing.startSmeltingFromItem(client, itemId, amount)
                true
            }
            interfaceId in 1119..1123 -> {
                SmithingInterface.startSmithingFromInterfaceSelection(client, interfaceId, itemId, slot, amount)
                true
            }
            interfaceId in 4233..4257 -> {
                Crafting.startGoldJewelry(client, GoldJewelryRequest(interfaceId, slot, amount))
                true
            }
            else -> false
        }
    }
}
