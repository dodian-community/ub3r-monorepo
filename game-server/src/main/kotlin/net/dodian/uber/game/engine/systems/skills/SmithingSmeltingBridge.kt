package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.api.content.ContentActions
import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.netty.listener.out.SendFrame27
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.skills.smithing.SmithingSmeltingService
import net.dodian.uber.skills.smithing.SmeltingRegistry

/** Protocol-facing adapter for the legacy furnace UI; recipe ownership remains in the plugin. */
object SmithingSmeltingBridge {
    private const val FURNACE_INTERFACE = 2400

    @JvmStatic
    fun open(client: Client) {
        SmeltingRegistry.recipes.forEachIndexed { index, recipe ->
            client.sendInterfaceModel(SmeltingRegistry.furnaceFrames()[index], 150, recipe.barId)
        }
        client.sendChatboxInterface(FURNACE_INTERFACE)
    }

    @JvmStatic fun isFurnaceFrame(interfaceId: Int): Boolean = SmeltingRegistry.isFurnaceFrame(interfaceId)
    @JvmStatic fun isFurnaceButton(buttonId: Int): Boolean = SmeltingRegistry.isFurnaceButton(buttonId)

    @JvmStatic
    fun startFromButton(client: Client, buttonId: Int): Boolean {
        val mapping = SmeltingRegistry.mapping(buttonId) ?: return false
        selectPendingRecipe(client, mapping.barId)
        return if (mapping.amount == 0) promptPendingX(client) else start(client, mapping.barId, mapping.amount)
    }

    @JvmStatic
    fun startFromInterfaceItem(client: Client, barId: Int, amount: Int): Boolean =
        start(client, barId, amount)

    @JvmStatic
    fun selectPendingRecipe(client: Client, barId: Int): Boolean {
        return SmithingSmeltingService.selectPending(client.asSkillPlayer(), barId)
    }

    @JvmStatic
    fun selectPendingRecipeFromOre(client: Client, itemId: Int): Boolean {
        val recipe = SmeltingRegistry.findRecipeByOre(itemId) ?: return false
        return SmithingSmeltingService.selectPending(client.asSkillPlayer(), recipe.barId)
    }

    @JvmStatic
    fun startFromPending(client: Client, amount: Int): Boolean =
        start(client, SmithingSmeltingService.pendingBar(client.asSkillPlayer()), amount)

    @JvmStatic
    fun promptX(client: Client, barId: Int): Boolean {
        if (!selectPendingRecipe(client, barId)) return false
        return promptPendingX(client)
    }

    @JvmStatic
    fun promptPendingX(client: Client): Boolean {
        if (SmeltingRegistry.findRecipe(SmithingSmeltingService.pendingBar(client.asSkillPlayer())) == null) return false
        client.enterAmountId = 2
        client.send(SendFrame27())
        return true
    }

    @JvmStatic
    fun clearSelection(client: Client) {
        SmithingSmeltingService.clear(client.asSkillPlayer(), ActionStopReason.USER_INTERRUPT)
    }

    private fun start(client: Client, barId: Int, amount: Int): Boolean {
        val recipe = SmeltingRegistry.findRecipe(barId) ?: return false
        if (client.getLevel(Skill.SMITHING) < recipe.levelRequired) {
            client.sendMessage("You need level ${recipe.levelRequired} smithing to do this!")
            return true
        }
        SmithingSmeltingService.selectPending(client.asSkillPlayer(), recipe.barId)
        client.send(RemoveInterfaces())
        ContentActions.cancel(
            client,
            PlayerActionCancelReason.NEW_ACTION,
            fullResetAnimation = false,
            closeInterfaces = false,
            resetCompatibilityState = false,
        )
        return SmithingSmeltingService.start(client.asSkillPlayer(), recipe.barId, amount)
    }
}
