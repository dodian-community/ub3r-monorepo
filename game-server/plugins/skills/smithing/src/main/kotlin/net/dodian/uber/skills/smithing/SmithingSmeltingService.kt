package net.dodian.uber.skills.smithing

import net.dodian.uber.game.api.content.ContentAttributeKey
import net.dodian.uber.game.api.plugin.skills.SkillActionHandle
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.skill.runtime.action.productionAction

/** Plugin-owned selection state and repeated action loop for the legacy furnace interface. */
object SmithingSmeltingService {
    private const val MODULE_ID = "skill.smithing"
    private const val ACTION_NAME = "smithing.smelt"
    private const val SMELT_ANIMATION = 0x383
    private const val SMELT_DELAY_TICKS = 3

    private val pendingBarKey = ContentAttributeKey<Int>(MODULE_ID, "pendingSmeltingBar")
    private val activeActionKey = ContentAttributeKey<SkillActionHandle>(MODULE_ID, "activeSmeltingAction")
    private val remainingKey = ContentAttributeKey<Int>(MODULE_ID, "smeltingRemaining")

    fun selectPending(player: SkillPlayer, barId: Int): Boolean {
        if (SmeltingRegistry.findRecipe(barId) == null) return false
        player.attributes.put(pendingBarKey, barId)
        return true
    }

    fun pendingBar(player: SkillPlayer): Int = player.attributes.get(pendingBarKey) ?: -1

    fun clearPending(player: SkillPlayer) {
        player.attributes.remove(pendingBarKey)
    }

    fun start(player: SkillPlayer, barId: Int, amount: Int): Boolean {
        val recipe = SmeltingRegistry.findRecipe(barId) ?: return false
        if (player.skills.current(Skill.SMITHING) < recipe.levelRequired) {
            player.ui.message("You need level ${recipe.levelRequired} smithing to do this!")
            return true
        }

        stopAction(player, ActionStopReason.USER_INTERRUPT)
        selectPending(player, recipe.barId)
        player.attributes.put(remainingKey, amount.coerceAtLeast(1))

        val handle = productionAction(ACTION_NAME) {
            delay(SMELT_DELAY_TICKS)
            onCycleSignal { performCycle(this, recipe) }
            onStop {
                attributes.remove(activeActionKey)
                attributes.remove(remainingKey)
            }
        }.start(player)

        if (handle == null) {
            player.attributes.remove(remainingKey)
            return false
        }
        player.attributes.put(activeActionKey, handle)
        return true
    }

    fun stopAction(player: SkillPlayer, reason: ActionStopReason = ActionStopReason.USER_INTERRUPT) {
        player.attributes.get(activeActionKey)?.cancel(reason)
        player.attributes.remove(activeActionKey)
        player.attributes.remove(remainingKey)
    }

    fun clear(player: SkillPlayer, reason: ActionStopReason = ActionStopReason.USER_INTERRUPT) {
        stopAction(player, reason)
        clearPending(player)
    }

    private fun performCycle(player: SkillPlayer, recipe: SmeltingRecipe): CycleSignal {
        val remaining = player.attributes.get(remainingKey)
            ?: return CycleSignal.stop(ActionStopReason.INVALID_TARGET)
        if (remaining <= 0) return CycleSignal.stop(ActionStopReason.COMPLETED)
        if (player.actions.busy) {
            player.ui.message("You are currently busy to be smelting!")
            return CycleSignal.stop(ActionStopReason.REQUIREMENT_FAILED)
        }
        if (player.skills.current(Skill.SMITHING) < recipe.levelRequired) {
            player.ui.message("You need level ${recipe.levelRequired} smithing to do this!")
            return CycleSignal.stop(ActionStopReason.REQUIREMENT_FAILED)
        }
        for (requirement in recipe.oreRequirements) {
            if (!player.inventory.contains(requirement.itemId, requirement.amount)) {
                player.ui.message(missingRequirementMessage(player, recipe, requirement))
                return CycleSignal.stop(ActionStopReason.REQUIREMENT_FAILED)
            }
        }

        val chance = recipe.successChancePercent + ((player.skills.current(Skill.SMITHING) + 1) / 4)
        val success = recipe.successChancePercent >= 100 || player.random.between(1, 100) <= chance
        val committed = player.inventory.transaction {
            recipe.oreRequirements.forEach { requirement -> remove(requirement.itemId, requirement.amount) }
            if (success) add(recipe.barId, 1)
        }
        if (!committed) return CycleSignal.stop(ActionStopReason.REQUIREMENT_FAILED)

        player.actions.animate(SMELT_ANIMATION, 0)
        if (success) {
            player.skills.gainXp(recipe.experience, Skill.SMITHING)
            player.actions.triggerRandomEvent(recipe.experience)
        } else {
            recipe.failureMessage?.let(player.ui::message)
        }

        val nextRemaining = remaining - 1
        player.attributes.put(remainingKey, nextRemaining)
        return if (nextRemaining <= 0) CycleSignal.completeSuccess() else CycleSignal.success()
    }

    private fun missingRequirementMessage(
        player: SkillPlayer,
        recipe: SmeltingRecipe,
        missing: OreRequirement,
    ): String = when (recipe.barId) {
        2349 -> "You need a tin and copper to do this!"
        2353 -> "You need a iron ore and 2 coal to do this!"
        2359 -> "You need a mithril ore and 3 coal to do this!"
        2361 -> "You need a adamantite ore and 4 coal to do this!"
        2363 -> "You need a runite ore and 6 coal to do this!"
        else -> "You need ${missing.amount} ${player.inventory.itemName(missing.itemId).lowercase()} to do this!"
    }
}
