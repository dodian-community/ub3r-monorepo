package net.dodian.uber.game.content.skills.smithing

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.content.skills.crafting.Crafting
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.api.content.ContentActions
import net.dodian.uber.game.systems.api.content.ContentProductionMode
import net.dodian.uber.game.systems.api.content.ContentProductionRequest
import net.dodian.uber.game.systems.action.PolicyPreset
import net.dodian.uber.game.systems.skills.ProgressionService
import net.dodian.uber.game.content.skills.runtime.action.SkillingRandomEventService
import net.dodian.uber.game.content.skills.runtime.action.ActionStopReason
import net.dodian.uber.game.content.skills.runtime.action.CycleSignal
import net.dodian.uber.game.systems.skills.plugin.SkillPlugin
import net.dodian.uber.game.systems.skills.SkillPolicyMetrics
import net.dodian.uber.game.systems.skills.SkillPolicyResult
import net.dodian.uber.game.systems.skills.SkillPolicyRoute
import net.dodian.uber.game.content.skills.runtime.action.productionAction
import net.dodian.uber.game.systems.skills.plugin.skillPlugin
import net.dodian.utilities.Range

object Smithing {
    private const val SMELT_ANIMATION = 0x383
    private const val SMELT_DELAY_TICKS = 3

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
        val selection = client.getSmeltingSelection() ?: return
        var remaining = selection.amount
        productionAction("smelting") {
            delay(SMELT_DELAY_TICKS)
            onCycleSignal {
                val current = getSmeltingSelection() ?: return@onCycleSignal CycleSignal.stop(ActionStopReason.INVALID_TARGET)
                if (!performCycle(this, current.recipe)) {
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
                clearSmeltingSelection()
            }
        }.start(client)
    }

    private fun performCycle(player: Client, recipe: SmeltingRecipe): Boolean {
        if (player.isBusy()) {
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
        player.performAnimation(SMELT_ANIMATION, 0)
        recipe.oreRequirements.forEach { requirement ->
            repeat(requirement.amount) {
                player.deleteItem(requirement.itemId, 1)
            }
        }
        val success = recipe.successChancePercent >= 100 || Range(1, 100).value <= recipe.successChancePercent + ((player.getLevel(Skill.SMITHING) + 1) / 4)
        if (success) {
            player.addItem(recipe.barId, 1)
            ProgressionService.addXp(player, recipe.experience, Skill.SMITHING)
            SkillingRandomEventService.trigger(player, recipe.experience)
        } else if (recipe.failureMessage != null) {
            player.sendMessage(recipe.failureMessage)
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
            else -> "You need ${missing.amount} ${player.getItemName(missing.itemId).lowercase()} to do this!"
        }
        return SendMessage(message)
    }

    @JvmStatic
    fun openSmelting(client: Client) = SmithingInterface.open(client)

    @JvmStatic
    fun startSmelting(client: Client, amount: Int) = SmithingInterface.startFromPending(client, amount)

    @JvmStatic
    fun startSmeltingFromItem(client: Client, itemId: Int, amount: Int) =
        SmithingInterface.startFromInterfaceItem(client, itemId, amount)

    @JvmStatic
    fun openSmithing(client: Client, barId: Int, anvilX: Int, anvilY: Int) =
        SmithingInterface.openForBar(client, barId, anvilX, anvilY)

    @JvmStatic
    fun startSmithing(client: Client, itemId: Int, amount: Int) =
        SmithingInterface.startSmithingFromInterfaceItem(client, itemId, amount)

    @JvmStatic
    fun castSuperheat(client: Client, itemId: Int) = Superheat.cast(client, itemId)
}

object SmithingObjectComponents {
    val anvilObjects = intArrayOf(2097, 2783)
    val furnaceObjects = intArrayOf(2150, 2151, 2152, 2153, 3994, 11666, 16469, 29662)
    val smeltingInterfaceFurnaces = intArrayOf(3994, 11666, 16469, 29662)
}

object AnvilObjects : ObjectContent {
    override val objectIds: IntArray = SmithingObjectComponents.anvilObjects

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId != 2097) {
            return false
        }
        val barId = SmithingInterface.firstBarInInventory(client)
        if (barId != -1) {
            client.setInteractionAnchor(position.x, position.y, position.z)
            Smithing.openSmithing(client, barId, position.x, position.y)
        } else {
            client.sendMessage("You do not have any bars to smith!")
        }
        return true
    }

    override fun onUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        if (objectId != 2097 && objectId != 2783) {
            return false
        }

        if (objectId == 2097 && (itemId == 1540 || itemId == 11286)) {
            if (!client.playerHasItem(2347)) {
                client.sendMessage("You need a hammer!")
            } else if (itemId == 1540 && !client.playerHasItem(11286)) {
                client.sendMessage("You need a draconic visage!")
            } else if (itemId == 11286 && !client.playerHasItem(1540)) {
                client.sendMessage("You need a anti-dragon shield!")
            } else if (client.getLevel(Skill.SMITHING) < 90) {
                client.sendMessage("You need level 90 smithing to do this!")
            } else {
                client.deleteItem(itemId, itemSlot, 1)
                client.deleteItem(if (itemId == 1540) 11286 else 1540, 1)
                client.addItemSlot(11284, 1, itemSlot)
                client.checkItemUpdate()
                ProgressionService.addXp(client, 15000, Skill.SMITHING)
                client.sendMessage("Your smithing craft made a Dragonfire shield out of the visage.")
            }
            return true
        }
        if (SmithingInterface.resolveTierId(itemId) != -1) {
            client.setInteractionAnchor(position.x, position.y, position.z)
            Smithing.openSmithing(client, itemId, position.x, position.y)
            return true
        }
        return false
    }
}

object FurnaceObjects : ObjectContent {
    override val objectIds: IntArray = SmithingObjectComponents.furnaceObjects

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId !in SmithingObjectComponents.smeltingInterfaceFurnaces) {
            return false
        }
        Smithing.openSmelting(client)
        return true
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId in SmithingObjectComponents.smeltingInterfaceFurnaces) {
            Smithing.openSmelting(client)
            return true
        }
        return false
    }

    override fun onUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        if (objectId !in SmithingObjectComponents.smeltingInterfaceFurnaces) {
            return false
        }
        if (itemId == 1783 || itemId == 1781) {
            client.send(RemoveInterfaces())
            if (!client.playerHasItem(1783) || !client.playerHasItem(1781)) {
                client.sendMessage("You need one bucket of sand and one soda ash")
                return true
            }
            ContentActions.queueProductionSelection(
                client,
                ContentProductionRequest(
                    skillId = Skill.CRAFTING.id,
                    productId = 1775,
                    amountPerCycle = 1,
                    primaryItemId = 1783,
                    secondaryItemId = 1781,
                    experiencePerUnit = 80,
                    animationId = 899,
                    tickDelay = 3,
                    completionMessage = "You smelt soda ash with the sand and made molten glass.",
                    mode = ContentProductionMode.MOLTEN_GLASS,
                ),
            )
            return true
        }

        if (itemId == 2357) {
            Crafting.openGoldJewelry(client)
            return true
        }
        SmithingInterface.selectPendingRecipeFromOre(client, itemId)
        Smithing.openSmelting(client)
        return true
    }

    override fun onMagic(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        spellId: Int,
    ): Boolean {
        return when {
            objectId == 2151 && spellId == 1179 -> chargeOrb(client, 55, 571, 725, "You charge the orb with the power of water.")
            objectId == 2150 && spellId == 1182 -> chargeOrb(client, 60, 575, 800, "You charge the orb with the power of earth.")
            objectId == 2153 && spellId == 1184 -> chargeOrb(client, 65, 569, 875, "You charge the orb with the power of fire.")
            objectId == 2152 && spellId == 1186 -> chargeOrb(client, 70, 573, 950, "You charge the orb with the power of air.")
            else -> false
        }
    }

    private fun chargeOrb(client: Client, levelReq: Int, resultItem: Int, exp: Int, message: String): Boolean {
        if (client.getLevel(Skill.MAGIC) < levelReq) {
            client.sendMessage("You need level $levelReq magic in order to cast this spell!")
            return true
        }
        if (!client.playerHasItem(567) || !client.playerHasItem(564, 3)) {
            client.sendMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk.")
            return true
        }
        ContentActions.queueProductionSelection(
            client,
            ContentProductionRequest(
                skillId = Skill.MAGIC.id,
                productId = resultItem,
                amountPerCycle = 1,
                primaryItemId = 567,
                secondaryItemId = 564,
                experiencePerUnit = exp,
                animationId = 726,
                tickDelay = 5,
                completionMessage = message,
                mode = ContentProductionMode.CHARGED_ORB,
            ),
        )
        return true
    }
}

object SmithingSkillPlugin : SkillPlugin {
    private val firstClickObjects =
        (AnvilObjects.objectIds + FurnaceObjects.objectIds).distinct().toIntArray()

    override val definition =
        skillPlugin(name = "Smithing", skill = Skill.SMITHING) {
            objectClick(preset = PolicyPreset.PRODUCTION, option = 1, *firstClickObjects) { client, objectId, position, obj ->
                if (objectId in AnvilObjects.objectIds) {
                    AnvilObjects.onFirstClick(client, objectId, position, obj)
                } else {
                    FurnaceObjects.onFirstClick(client, objectId, position, obj)
                }
            }
            objectClick(preset = PolicyPreset.PRODUCTION, option = 2, *FurnaceObjects.objectIds) { client, objectId, position, obj ->
                FurnaceObjects.onSecondClick(client, objectId, position, obj)
            }
        }
}
