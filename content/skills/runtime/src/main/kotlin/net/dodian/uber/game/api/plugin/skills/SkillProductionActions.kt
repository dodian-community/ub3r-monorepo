package net.dodian.uber.game.api.plugin.skills

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.skill.runtime.action.productionAction
import net.dodian.uber.skills.api.SkillRecipe

fun SkillPlayer.startProduction(recipe: SkillRecipe, amount: Int, skill: Skill): SkillActionHandle? {
    if (amount <= 0) return null
    var remaining = amount
    return productionAction("production.${recipe.key}") {
        delay(recipe.delayTicks)
        requirements {
            level(skill, recipe.requiredLevel)
            recipe.materials.forEach { item(it.itemId, it.amount, recipe.missingMaterialsMessage) }
            if (recipe.requiredFreeSlots > 0) inventorySpace(recipe.requiredFreeSlots, "You need more inventory space.")
            if (recipe.toolItemIds.isNotEmpty()) requirement {
                if (recipe.toolItemIds.any { inventory.contains(it) || equipment.item(SkillEquipmentSlot.WEAPON) == it }) SkillValidationResult.ok()
                else SkillValidationResult.failed("You need the required tool to continue.")
            }
            if (recipe.requiresPremium) requirement {
                if (profile.premium) SkillValidationResult.ok() else SkillValidationResult.failed("This action requires premium access.")
            }
        }
        onCycleSignal {
            if (remaining <= 0) return@onCycleSignal CycleSignal.stop(ActionStopReason.COMPLETED)
            val committed = inventory.transaction {
                recipe.materials.forEach { remove(it.itemId, it.amount) }
                add(recipe.outputItemId, recipe.outputAmount)
                recipe.byproducts.forEach { add(it.itemId, it.amount) }
            }
            if (!committed) return@onCycleSignal CycleSignal.stop(ActionStopReason.REQUIREMENT_FAILED)
            if (recipe.animationId >= 0) actions.animate(recipe.animationId)
            remaining--; if (remaining <= 0) CycleSignal.completeSuccess() else CycleSignal.success()
        }
        onSuccess { if (recipe.experience > 0) { skills.gainXp(recipe.experience, skill); actions.triggerRandomEvent(recipe.experience) }; recipe.successMessage?.let(ui::message) }
    }.start(this)
}
