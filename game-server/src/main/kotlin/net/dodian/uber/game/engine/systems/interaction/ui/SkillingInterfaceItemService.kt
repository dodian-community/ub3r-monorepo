package net.dodian.uber.game.engine.systems.interaction.ui

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.skills.SmithingAnvilBridge
import net.dodian.uber.game.engine.systems.skills.SkillInteractionDispatcher
import net.dodian.uber.game.engine.systems.skills.SmithingSmeltingBridge

object SkillingInterfaceItemService {
    @JvmStatic
    fun handleContainerAmount(
        client: Client,
        interfaceId: Int,
        itemId: Int,
        slot: Int,
        amount: Int,
    ): Boolean {
        if (SkillInteractionDispatcher.tryHandleItemGrid(client, interfaceId, itemId, slot, amount)) return true
        return when {
            SmithingSmeltingBridge.isFurnaceFrame(interfaceId) -> {
                SmithingSmeltingBridge.startFromInterfaceItem(client, itemId, amount)
                true
            }
            interfaceId in 1119..1123 -> {
                SmithingAnvilBridge.startFromInterfaceSelection(client, interfaceId, itemId, slot, amount)
                true
            }
            else -> false
        }
    }
}
