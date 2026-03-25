package net.dodian.uber.game.content.items.combination

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.action.ProductionActionService
import net.dodian.uber.game.runtime.action.ProductionRequest
import net.dodian.uber.game.skills.fletching.FletchingDefinitions
import net.dodian.uber.game.skills.crafting.CraftingPlugin
import net.dodian.uber.game.skills.fletching.FletchingPlugin

object FletchingItemCombinationHandler {
    @JvmStatic
    fun handle(client: Client, itemUsed: Int, otherItem: Int, knife: Boolean): Boolean {
        if (knife && (itemUsed == 1511 || otherItem == 1511)) {
            client.resetAction()
            CraftingPlugin.startShafting(client)
            return true
        }
        if ((itemUsed == 314 || otherItem == 314) && (itemUsed == 52 || otherItem == 52)) {
            ProductionActionService.queueSelection(
                client,
                ProductionRequest(
                    skillId = Skill.FLETCHING.id,
                    productId = 53,
                    amountPerCycle = 15,
                    primaryItemId = itemUsed,
                    secondaryItemId = otherItem,
                    experiencePerUnit = 5,
                    animationId = -1,
                    tickDelay = 2,
                ),
            )
            return true
        }
        for (recipe in FletchingDefinitions.dartRecipes) {
            if ((itemUsed == recipe.tipId || otherItem == recipe.tipId) && (itemUsed == 314 || otherItem == 314)) {
                client.resetAction()
                if (client.getLevel(Skill.FLETCHING) < recipe.requiredLevel) {
                    client.send(SendMessage("You need level ${recipe.requiredLevel} fletcing to make ${client.GetItemName(recipe.dartId).lowercase()}"))
                    return true
                }
                if (!client.playerHasItem(recipe.dartId) && client.freeSlots() < 1) {
                    client.send(SendMessage("Your inventory is full!"))
                    return true
                }
                ProductionActionService.queueSelection(
                    client,
                    ProductionRequest(
                        skillId = Skill.FLETCHING.id,
                        productId = recipe.dartId,
                        amountPerCycle = 10,
                        primaryItemId = itemUsed,
                        secondaryItemId = otherItem,
                        experiencePerUnit = recipe.experience / 2,
                        animationId = -1,
                        tickDelay = 3,
                    ),
                )
                return true
            }
        }
        for (recipe in FletchingDefinitions.arrowRecipes) {
            if ((itemUsed == recipe.headId || otherItem == recipe.headId) && (itemUsed == 53 || otherItem == 53)) {
                client.resetAction()
                if (client.getLevel(Skill.FLETCHING) < recipe.requiredLevel) {
                    client.send(SendMessage("Requires level ${recipe.requiredLevel} fletching"))
                    return true
                }
                if (!client.playerHasItem(recipe.arrowId) && client.freeSlots() < 1) {
                    client.send(SendMessage("Your inventory is full!"))
                    return true
                }
                ProductionActionService.queueSelection(
                    client,
                    ProductionRequest(
                        skillId = Skill.FLETCHING.id,
                        productId = recipe.arrowId,
                        amountPerCycle = 15,
                        primaryItemId = itemUsed,
                        secondaryItemId = otherItem,
                        experiencePerUnit = recipe.experience,
                        animationId = -1,
                        tickDelay = 3,
                        completionMessage = "You fletched some ${client.GetItemName(recipe.arrowId).lowercase()}.",
                    ),
                )
                return true
            }
        }
        FletchingDefinitions.bowLogs.forEachIndexed { index, bowLog ->
            if ((itemUsed == bowLog.logItemId || otherItem == bowLog.logItemId) && knife) {
                FletchingPlugin.open(client, index)
                return true
            }
        }
        for (bowLog in FletchingDefinitions.bowLogs) {
            if ((itemUsed == bowLog.unstrungShortbowId || otherItem == bowLog.unstrungShortbowId) && (itemUsed == 1777 || otherItem == 1777)) {
                client.resetAction()
                if (client.getLevel(Skill.FLETCHING) < bowLog.shortLevelRequired) {
                    client.send(SendMessage("Requires level ${bowLog.shortLevelRequired} fletching"))
                    return true
                }
                ProductionActionService.queueSelection(
                    client,
                    ProductionRequest(
                        skillId = Skill.FLETCHING.id,
                        productId = bowLog.shortbowId,
                        amountPerCycle = 1,
                        primaryItemId = itemUsed,
                        secondaryItemId = otherItem,
                        experiencePerUnit = bowLog.shortExperience,
                        animationId = bowLog.shortStringAnimationId,
                        tickDelay = 2,
                        completionMessage = "You string your ${client.GetItemName(bowLog.unstrungShortbowId).lowercase()} into a ${client.GetItemName(bowLog.shortbowId).lowercase()}.",
                    ),
                )
                return true
            }
        }
        for (bowLog in FletchingDefinitions.bowLogs) {
            if ((itemUsed == bowLog.unstrungLongbowId || otherItem == bowLog.unstrungLongbowId) && (itemUsed == 1777 || otherItem == 1777)) {
                client.resetAction()
                if (client.getLevel(Skill.FLETCHING) < bowLog.longLevelRequired) {
                    client.send(SendMessage("Requires level ${bowLog.longLevelRequired} fletching"))
                    return true
                }
                ProductionActionService.queueSelection(
                    client,
                    ProductionRequest(
                        skillId = Skill.FLETCHING.id,
                        productId = bowLog.longbowId,
                        amountPerCycle = 1,
                        primaryItemId = itemUsed,
                        secondaryItemId = otherItem,
                        experiencePerUnit = bowLog.longExperience,
                        animationId = bowLog.longStringAnimationId,
                        tickDelay = 2,
                        completionMessage = "You string your ${client.GetItemName(bowLog.unstrungLongbowId).lowercase()} into a ${client.GetItemName(bowLog.longbowId).lowercase()}.",
                    ),
                )
                return true
            }
        }
        return false
    }
}
