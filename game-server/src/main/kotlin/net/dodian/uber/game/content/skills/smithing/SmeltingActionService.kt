package net.dodian.uber.game.content.skills.smithing

import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.action.PlayerActionController
import net.dodian.uber.game.systems.action.PlayerActionType
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.content.skills.core.runtime.SkillingRandomEventService
import net.dodian.utilities.Range

object SmeltingActionService {
    private const val SMELT_ANIMATION = 0x383
    private const val SMELT_DELAY_TICKS = 3

    @JvmStatic
    fun start(client: Client) {
        val selection = client.getSmeltingSelection() ?: return
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.SMELTING,
            onStop = { player, _ -> player.clearSmeltingSelection() },
        ) {
            var remaining = selection.amount
            while (remaining > 0) {
                if (!isActive()) {
                    return@start
                }
                val current = player.getSmeltingSelection() ?: return@start
                if (!performCycle(player, current.recipe)) {
                    return@start
                }
                remaining--
                if (remaining <= 0) {
                    return@start
                }
                wait(SMELT_DELAY_TICKS)
            }
        }
    }

    private fun performCycle(player: Client, recipe: SmeltingRecipe): Boolean {
        if (player.isBusy()) {
            player.send(SendMessage("You are currently busy to be smelting!"))
            return false
        }
        if (player.getLevel(Skill.SMITHING) < recipe.levelRequired) {
            player.send(SendMessage("You need level ${recipe.levelRequired} smithing to do this!"))
            return false
        }
        for (requirement in recipe.oreRequirements) {
            if (!player.playerHasItem(requirement.itemId, requirement.amount)) {
                player.send(missingRequirementMessage(player, recipe, requirement))
                return false
            }
        }
        player.requestAnim(SMELT_ANIMATION, 0)
        recipe.oreRequirements.forEach { requirement ->
            repeat(requirement.amount) {
                player.deleteItem(requirement.itemId, 1)
            }
        }
        val success = recipe.successChancePercent >= 100 || Range(1, 100).value <= recipe.successChancePercent + ((player.getLevel(Skill.SMITHING) + 1) / 4)
        if (success) {
            player.addItem(recipe.barId, 1)
            SkillProgressionService.gainXp(player, recipe.experience, Skill.SMITHING)
            SkillingRandomEventService.trigger(player, recipe.experience)
        } else if (recipe.failureMessage != null) {
            player.send(SendMessage(recipe.failureMessage))
        }
        player.checkItemUpdate()
        return true
    }

    private fun missingRequirementMessage(player: Client, recipe: SmeltingRecipe, missing: OreRequirement): SendMessage {
        val message = when (recipe.barId) {
            2349 -> "You need a tin and copper to do this!"
            2353 -> "You need a iron ore and 2 coal to do this!"
            2359 -> "You need a mithril ore and 3 coal to do this!"
            2361 -> "You need a adamantite ore and 4 coal to do this!"
            2363 -> "You need a runite ore and 6 coal to do this!"
            else -> "You need ${missing.amount} ${player.GetItemName(missing.itemId).lowercase()} to do this!"
        }
        return SendMessage(message)
    }
}
