package net.dodian.uber.game.skills.smithing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendFrame27
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService
import org.slf4j.LoggerFactory

object SmeltingInterfaceService {
    private val logger = LoggerFactory.getLogger(SmeltingInterfaceService::class.java)
    private val furnaceFrameIds = SmithingDefinitions.frameIds()

    @JvmStatic
    fun open(client: Client) {
        logger.warn("Open smelting interface iface=2400 player={}", client.playerName)
        SmithingDefinitions.smeltingRecipes.forEachIndexed { index, recipe ->
            client.sendFrame246(furnaceFrameIds[index], 150, recipe.barId)
        }
        client.sendFrame164(2400)
    }

    @JvmStatic
    fun isSmeltingInterfaceFrame(interfaceId: Int): Boolean = furnaceFrameIds.contains(interfaceId)

    @JvmStatic
    fun selectPendingRecipeFromFrameButton(client: Client, buttonId: Int): Boolean {
        val frameIndex = furnaceFrameIds.indexOf(buttonId)
        if (frameIndex == -1) {
            logger.warn("Smelting frame button missed buttonId={} player={}", buttonId, client.playerName)
            return false
        }
        val recipe = SmithingDefinitions.smeltingRecipes.getOrNull(frameIndex) ?: return false
        logger.warn(
            "Smelting frame selected buttonId={} frameIndex={} barId={} player={}",
            buttonId,
            frameIndex,
            recipe.barId,
            client.playerName
        )
        client.setPendingSmeltingBarId(recipe.barId)
        return true
    }

    @JvmStatic
    fun startFromButton(client: Client, buttonId: Int): Boolean {
        val mapping = SmithingDefinitions.findSmeltingButton(buttonId) ?: return false
        val recipe = SmithingDefinitions.findSmeltingRecipe(mapping.barId) ?: return false
        logger.warn(
            "Smelting direct button buttonId={} barId={} amount={} player={}",
            buttonId,
            recipe.barId,
            mapping.amount,
            client.playerName
        )
        client.setPendingSmeltingBarId(recipe.barId)
        return if (mapping.amount <= 0) {
            promptPendingX(client)
        } else {
            start(client, recipe, mapping.amount)
        }
    }

    @JvmStatic
    fun startFromInterfaceItem(client: Client, barId: Int, amount: Int): Boolean {
        val recipe = SmithingDefinitions.findSmeltingRecipe(barId) ?: return false
        logger.warn(
            "Smelting interface item barId={} amount={} pendingBefore={} player={}",
            barId,
            amount,
            client.getPendingSmeltingBarId(),
            client.playerName
        )
        client.setPendingSmeltingBarId(recipe.barId)
        return start(client, recipe, amount)
    }

    @JvmStatic
    fun selectPendingRecipe(client: Client, barId: Int): Boolean {
        val recipe = SmithingDefinitions.findSmeltingRecipe(barId) ?: return false
        logger.warn("Smelting pending selected barId={} player={}", recipe.barId, client.playerName)
        client.setPendingSmeltingBarId(recipe.barId)
        return true
    }

    @JvmStatic
    fun selectPendingRecipeFromOre(client: Client, itemId: Int): Boolean {
        val recipe = SmithingDefinitions.findSmeltingRecipeByOre(itemId) ?: return false
        logger.warn(
            "Smelting pending selected from ore oreId={} barId={} player={}",
            itemId,
            recipe.barId,
            client.playerName
        )
        client.setPendingSmeltingBarId(recipe.barId)
        return true
    }

    @JvmStatic
    fun startFromPending(client: Client, amount: Int): Boolean {
        val recipe = SmithingDefinitions.findSmeltingRecipe(client.getPendingSmeltingBarId()) ?: return false
        logger.warn(
            "Smelting start from pending barId={} amount={} player={}",
            recipe.barId,
            amount,
            client.playerName
        )
        return start(client, recipe, amount)
    }

    @JvmStatic
    fun promptPendingX(client: Client): Boolean {
        if (SmithingDefinitions.findSmeltingRecipe(client.getPendingSmeltingBarId()) == null) {
            logger.warn(
                "Smelting X prompt rejected pendingBarId={} player={}",
                client.getPendingSmeltingBarId(),
                client.playerName
            )
            return false
        }
        logger.warn(
            "Smelting X prompt pendingBarId={} player={}",
            client.getPendingSmeltingBarId(),
            client.playerName
        )
        client.enterAmountId = 2
        client.send(SendFrame27())
        return true
    }

    private fun start(client: Client, recipe: SmeltingRecipe, amount: Int): Boolean {
        logger.warn(
            "Smelting start request barId={} amount={} levelReq={} smithingLvl={} player={}",
            recipe.barId,
            amount,
            recipe.levelRequired,
            client.getLevel(Skill.SMITHING),
            client.playerName
        )
        if (client.getLevel(Skill.SMITHING) < recipe.levelRequired) {
            client.send(SendMessage("You need level ${recipe.levelRequired} smithing to do this!"))
            return true
        }
        client.setPendingSmeltingBarId(recipe.barId)
        client.send(RemoveInterfaces())
        PlayerActionCancellationService.cancel(
            client,
            PlayerActionCancelReason.NEW_ACTION,
            fullResetAnimation = false,
            closeInterfaces = false,
            resetCompatibilityState = false,
        )
        client.setSmeltingSelection(SmeltingSelection(recipe, amount.coerceAtLeast(1)))
        SmeltingActionService.start(client)
        return true
    }
}
