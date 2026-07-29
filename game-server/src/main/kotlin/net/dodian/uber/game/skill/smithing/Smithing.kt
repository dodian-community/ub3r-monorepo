package net.dodian.uber.game.skill.smithing

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.skill.crafting.Crafting
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.api.content.ContentActions
import net.dodian.uber.game.api.content.ContentProductionMode
import net.dodian.uber.game.api.content.ContentProductionRequest
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.game.engine.systems.inventory.inventoryTransaction
import net.dodian.uber.game.skill.runtime.action.SkillingRandomEventService
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.skills.SkillPolicyMetrics
import net.dodian.uber.game.engine.systems.skills.SkillPolicyResult
import net.dodian.uber.game.engine.systems.skills.SkillPolicyRoute
import net.dodian.uber.game.engine.systems.skills.SmithingSmeltingBridge
import net.dodian.uber.game.skill.runtime.action.productionAction
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.utilities.Range
import net.dodian.uber.skills.smithing.SmeltingRecipe

object Smithing {
    private const val SMELT_ANIMATION = 0x383
    private const val SMELT_DELAY_TICKS = 3
    private const val SMELTING_INTERFACE_ID = 2400

    @JvmStatic
    fun start(client: Client) = startSmelting(client)

    @JvmStatic
    fun startAction(client: Client) = startSmelting(client)

    @JvmStatic
    fun stopAction(client: Client) {
        client.clearSmeltingSelection()
        client.clearPendingSmeltingBarId()
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun stopFromReset(client: Client, fullReset: Boolean) {
        stopAction(client)
    }

    @JvmStatic
    fun startSmelting(client: Client) {
        val selection = client.smeltingSelection ?: return
        var remaining = selection.amount
        productionAction("smelting") {
            delay(SMELT_DELAY_TICKS)
            onCycleSignal {
                val current = client.smeltingSelection ?: return@onCycleSignal CycleSignal.stop(ActionStopReason.INVALID_TARGET)
                if (!performCycle(client, current.recipe)) {
                    SkillPolicyMetrics.record(PolicyPreset.PRODUCTION, SkillPolicyRoute.ACTION_CYCLE, SkillPolicyResult.POLICY_REJECT)
                    return@onCycleSignal CycleSignal.stop(ActionStopReason.REQUIREMENT_FAILED)
                }
                SkillPolicyMetrics.record(PolicyPreset.PRODUCTION, SkillPolicyRoute.ACTION_CYCLE, SkillPolicyResult.HANDLED)
                remaining--
                if (remaining <= 0) {
                    CycleSignal.stop(ActionStopReason.COMPLETED)
                } else {
                    CycleSignal.success()
                }
            }
            onStop {
                if (it != ActionStopReason.COMPLETED) {
                    SkillPolicyMetrics.record(PolicyPreset.PRODUCTION, SkillPolicyRoute.ACTION_CYCLE, SkillPolicyResult.CANCELLED)
                }
                client.clearSmeltingSelection()
            }
        }.start(client.asSkillPlayer())
    }

    private fun performCycle(player: Client, recipe: SmeltingRecipe): Boolean {
        if (player.isBusy) {
            player.sendMessage("You are currently busy to be smelting!")
            return false
        }
        if (player.getLevel(Skill.SMITHING) < recipe.levelRequired) {
            player.sendMessage("You need level ${recipe.levelRequired} smithing to do this!")
            return false
        }
        for (requirement in recipe.oreRequirements) {
            if (!player.playerHasItem(requirement.itemId, requirement.amount)) {
                player.send(missingRequirementMessage(player, recipe, requirement))
                return false
            }
        }
        val success = recipe.successChancePercent >= 100 || Range(1, 100).value <= recipe.successChancePercent + ((player.getLevel(Skill.SMITHING) + 1) / 4)
        val committed = player.inventoryTransaction {
            recipe.oreRequirements.forEach { requirement -> remove(requirement.itemId, requirement.amount) }
            if (success) add(recipe.barId, 1)
        }
        if (!committed) return false
        player.performAnimation(SMELT_ANIMATION, 0)
        if (success) {
            ProgressionService.addXp(player, recipe.experience, Skill.SMITHING)
            SkillingRandomEventService.trigger(player, recipe.experience)
        } else if (recipe.failureMessage != null) {
            player.sendMessage(recipe.failureMessage)
        }
        return true
    }

    private fun missingRequirementMessage(player: Client, recipe: SmeltingRecipe, missing: net.dodian.uber.skills.smithing.OreRequirement): SendMessage {
        val message = when (recipe.barId) {
            2349 -> "You need a tin and copper to do this!"
            2353 -> "You need a iron ore and 2 coal to do this!"
            2359 -> "You need a mithril ore and 3 coal to do this!"
            2361 -> "You need a adamantite ore and 4 coal to do this!"
            2363 -> "You need a runite ore and 6 coal to do this!"
            else -> "You need ${missing.amount} ${player.getItemName(missing.itemId).lowercase()} to do this!"
        }
        return SendMessage(message)
    }

    @JvmStatic
    fun openSmelting(client: Client) = SmithingSmeltingBridge.open(client)

    @JvmStatic
    fun startSmelting(client: Client, amount: Int) = SmithingSmeltingBridge.startFromPending(client, amount)

    @JvmStatic
    fun startSmeltingFromItem(client: Client, itemId: Int, amount: Int) =
        SmithingSmeltingBridge.startFromInterfaceItem(client, itemId, amount)

    @JvmStatic
    fun openSmithing(client: Client, barId: Int, anvilX: Int, anvilY: Int) =
        SmithingInterface.openForBar(client, barId, anvilX, anvilY)

    @JvmStatic
    fun startSmithing(client: Client, itemId: Int, amount: Int) =
        SmithingInterface.startSmithingFromInterfaceItem(client, itemId, amount)

    @JvmStatic
    fun castSuperheat(client: Client, itemId: Int) = Superheat.cast(client, itemId)

    @JvmStatic
    fun smeltingInterfaceId(): Int = SMELTING_INTERFACE_ID
}

object SmithingObjectComponents {
    val anvilObjects = intArrayOf(2097, 2783)
    val furnaceObjects = intArrayOf(2150, 2151, 2152, 2153, 3994, 2030, 16469, 29662)
    val smeltingInterfaceFurnaces = intArrayOf(3994, 2030, 16469, 29662)
}
