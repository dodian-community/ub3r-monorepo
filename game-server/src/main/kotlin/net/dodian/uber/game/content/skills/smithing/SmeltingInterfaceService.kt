package net.dodian.uber.game.content.skills.smithing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
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
    fun startFromButton(client: Client, buttonId: Int): Boolean {
        val mapping = SmithingDefinitions.findSmeltingButton(buttonId) ?: return false
        val recipe = SmithingDefinitions.findSmeltingRecipe(mapping.barId) ?: return false
        if (client.getLevel(Skill.SMITHING) < recipe.levelRequired) {
            client.send(SendMessage("You need level ${recipe.levelRequired} smithing to do this!"))
            return true
        }
        client.send(RemoveInterfaces())
        PlayerActionCancellationService.cancel(
            client,
            PlayerActionCancelReason.NEW_ACTION,
            fullResetAnimation = false,
            closeInterfaces = false,
            resetCompatibilityState = false,
        )
        client.setSmeltingSelection(SmeltingSelection(recipe, mapping.amount))
        SmeltingActionService.start(client)
        return true
    }
}
