package net.dodian.uber.game.skills.smithing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendFrame27
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService
object SmeltingInterfaceService {
    private val furnaceFrameIds = SmithingDefinitions.frameIds()

    @JvmStatic
    fun open(client: Client) {
        SmithingDefinitions.smeltingRecipes.forEachIndexed { index, recipe ->
            client.sendFrame246(furnaceFrameIds[index], 150, recipe.barId)
        }
        client.sendFrame164(2400)
    }

    @JvmStatic
    fun isSmeltingInterfaceFrame(interfaceId: Int): Boolean = furnaceFrameIds.contains(interfaceId)

    @JvmStatic
    fun startFromMapping(client: Client, mapping: FurnaceButtonMapping): Boolean {
        val recipe = SmithingDefinitions.findSmeltingRecipe(mapping.barId) ?: return false
        client.setPendingSmeltingBarId(recipe.barId)
        return if (mapping.amount <= 0) {
            promptPendingX(client)
        } else {
            start(client, SmeltingRequest(recipe, mapping.amount))
        }
    }

    @JvmStatic
    fun startFromInterfaceItem(client: Client, barId: Int, amount: Int): Boolean {
        val recipe = SmithingDefinitions.findSmeltingRecipe(barId) ?: return false
        client.setPendingSmeltingBarId(recipe.barId)
        return start(client, SmeltingRequest(recipe, amount))
    }

    @JvmStatic
    fun selectPendingRecipe(client: Client, barId: Int): Boolean {
        val recipe = SmithingDefinitions.findSmeltingRecipe(barId) ?: return false
        client.setPendingSmeltingBarId(recipe.barId)
        return true
    }

    @JvmStatic
    fun selectPendingRecipeFromOre(client: Client, itemId: Int): Boolean {
        val recipe = SmithingDefinitions.findSmeltingRecipeByOre(itemId) ?: return false
        client.setPendingSmeltingBarId(recipe.barId)
        return true
    }

    @JvmStatic
    fun startFromPending(client: Client, amount: Int): Boolean {
        val recipe = SmithingDefinitions.findSmeltingRecipe(client.getPendingSmeltingBarId()) ?: return false
        return start(client, SmeltingRequest(recipe, amount))
    }

    @JvmStatic
    fun promptPendingX(client: Client): Boolean {
        if (SmithingDefinitions.findSmeltingRecipe(client.getPendingSmeltingBarId()) == null) return false
        client.enterAmountId = 2
        client.send(SendFrame27())
        return true
    }

    private fun start(client: Client, request: SmeltingRequest): Boolean {
        if (client.getLevel(Skill.SMITHING) < request.recipe.levelRequired) {
            client.send(SendMessage("You need level ${request.recipe.levelRequired} smithing to do this!"))
            return true
        }
        client.setPendingSmeltingBarId(request.recipe.barId)
        client.send(RemoveInterfaces())
        PlayerActionCancellationService.cancel(
            client,
            PlayerActionCancelReason.NEW_ACTION,
            fullResetAnimation = false,
            closeInterfaces = false,
            resetCompatibilityState = false,
        )
        client.setSmeltingSelection(SmeltingSelection(request.recipe, request.amount.coerceAtLeast(1)))
        SmeltingActionService.start(client)
        return true
    }
}
