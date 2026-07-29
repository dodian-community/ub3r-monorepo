package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.skills.smithing.SmithingAnvilService

/** Client/protocol adapter for plugin-owned anvil selection and UI behavior. */
object SmithingAnvilBridge {
    @JvmStatic
    fun openForBar(client: Client, barId: Int, anvilX: Int, anvilY: Int): Boolean =
        SmithingAnvilService.openForBar(client.asSkillPlayer(), barId, anvilX, anvilY)

    @JvmStatic
    fun canSmithProduct(itemId: Int): Boolean = SmithingAnvilService.canSmithProduct(itemId)

    @JvmStatic
    fun startFromInterfaceSelection(
        client: Client,
        interfaceId: Int,
        itemId: Int,
        slot: Int,
        amount: Int,
    ): Boolean {
        val request = SmithingAnvilService.resolveRequest(
            player = client.asSkillPlayer(),
            interfaceId = interfaceId,
            itemId = itemId,
            slot = slot,
            amount = amount,
            fallbackAnchorX = client.interactionAnchorX,
            fallbackAnchorY = client.interactionAnchorY,
        ) ?: return true
        return SmithingAnvilService.start(client.asSkillPlayer(), request)
    }

    @JvmStatic
    fun firstBarInInventory(client: Client): Int =
        SmithingAnvilService.firstBarInInventory(client.asSkillPlayer())

    @JvmStatic
    fun resetRuntimeState(client: Client) {
        SmithingAnvilService.clear(client.asSkillPlayer())
    }
}
