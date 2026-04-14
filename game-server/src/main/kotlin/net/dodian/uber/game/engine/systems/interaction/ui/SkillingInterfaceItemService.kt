package net.dodian.uber.game.engine.systems.interaction.ui

import net.dodian.uber.game.skill.crafting.Crafting
import net.dodian.uber.game.skill.crafting.GoldJewelryRequest
import net.dodian.uber.game.skill.smithing.SmithingInterface
import net.dodian.uber.game.skill.smithing.Smithing
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
