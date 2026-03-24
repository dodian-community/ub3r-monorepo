package net.dodian.uber.game.skills.core

import net.dodian.uber.game.model.entity.player.Client
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
                SmeltingInterfaceService.startFromInterfaceItem(client, itemId, amount)
                true
            }
            interfaceId in 1119..1123 -> {
                SmithingInterfaceService.startFromInterfaceItem(client, itemId, amount)
                true
            }
            interfaceId in 4233..4257 -> {
                client.startGoldCrafting(interfaceId, slot, amount)
                true
            }
            else -> false
        }
    }
}
