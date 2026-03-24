package net.dodian.uber.game.content.items.combination

import net.dodian.uber.game.Constants
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.action.ProductionActionService
import net.dodian.uber.game.runtime.action.ProductionRequest
import net.dodian.uber.game.runtime.action.SkillingActionService
import net.dodian.uber.game.skills.fletching.FletchingService

object FletchingItemCombinationHandler {
    @JvmStatic
    fun handle(client: Client, itemUsed: Int, otherItem: Int, knife: Boolean): Boolean {
        if (knife && (itemUsed == 1511 || otherItem == 1511)) {
            client.resetAction()
            net.dodian.uber.game.skills.crafting.CraftingService.startShafting(client)
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
        for (index in Constants.darttip.indices) {
            if ((itemUsed == Constants.darttip[index] || otherItem == Constants.darttip[index]) && (itemUsed == 314 || otherItem == 314)) {
                client.resetAction()
                if (client.getLevel(Skill.FLETCHING) < Constants.darttip_required[index]) {
                    client.send(SendMessage("You need level ${Constants.darttip_required[index]} fletcing to make ${client.GetItemName(Constants.darts[index]).lowercase()}"))
                    return true
                }
                if (!client.playerHasItem(Constants.darts[index]) && client.freeSlots() < 1) {
                    client.send(SendMessage("Your inventory is full!"))
                    return true
                }
                ProductionActionService.queueSelection(
                    client,
                    ProductionRequest(
                        skillId = Skill.FLETCHING.id,
                        productId = Constants.darts[index],
                        amountPerCycle = 10,
                        primaryItemId = itemUsed,
                        secondaryItemId = otherItem,
                        experiencePerUnit = Constants.darttip_xp[index] / 2,
                        animationId = -1,
                        tickDelay = 3,
                    ),
                )
                return true
            }
        }
        for (index in Constants.heads.indices) {
            if ((itemUsed == Constants.heads[index] || otherItem == Constants.heads[index]) && (itemUsed == 53 || otherItem == 53)) {
                client.resetAction()
                if (client.getLevel(Skill.FLETCHING) < Constants.required[index]) {
                    client.send(SendMessage("Requires level ${Constants.required[index]} fletching"))
                    return true
                }
                if (!client.playerHasItem(Constants.arrows[index]) && client.freeSlots() < 1) {
                    client.send(SendMessage("Your inventory is full!"))
                    return true
                }
                ProductionActionService.queueSelection(
                    client,
                    ProductionRequest(
                        skillId = Skill.FLETCHING.id,
                        productId = Constants.arrows[index],
                        amountPerCycle = 15,
                        primaryItemId = itemUsed,
                        secondaryItemId = otherItem,
                        experiencePerUnit = Constants.xp[index],
                        animationId = -1,
                        tickDelay = 3,
                        completionMessage = "You fletched some ${client.GetItemName(Constants.arrows[index]).lowercase()}.",
                    ),
                )
                return true
            }
        }
        for (index in Constants.logs.indices) {
            if ((itemUsed == Constants.logs[index] || otherItem == Constants.logs[index]) && knife) {
                FletchingService.openBowSelection(client, index)
                return true
            }
        }
        for (index in Constants.shortbow.indices) {
            if ((itemUsed == Constants.shortbows[index] || otherItem == Constants.shortbows[index]) && (itemUsed == 1777 || otherItem == 1777)) {
                client.resetAction()
                if (client.getLevel(Skill.FLETCHING) < Constants.shortreq[index]) {
                    client.send(SendMessage("Requires level ${Constants.shortreq[index]} fletching"))
                    return true
                }
                ProductionActionService.queueSelection(
                    client,
                    ProductionRequest(
                        skillId = Skill.FLETCHING.id,
                        productId = Constants.shortbow[index],
                        amountPerCycle = 1,
                        primaryItemId = itemUsed,
                        secondaryItemId = otherItem,
                        experiencePerUnit = Constants.shortexp[index],
                        animationId = 6679 + index,
                        tickDelay = 2,
                        completionMessage = "You string your ${client.GetItemName(Constants.shortbows[index]).lowercase()} into a ${client.GetItemName(Constants.shortbow[index]).lowercase()}.",
                    ),
                )
                return true
            }
        }
        for (index in Constants.longbows.indices) {
            if ((itemUsed == Constants.longbows[index] || otherItem == Constants.longbows[index]) && (itemUsed == 1777 || otherItem == 1777)) {
                client.resetAction()
                if (client.getLevel(Skill.FLETCHING) < Constants.longreq[index]) {
                    client.send(SendMessage("Requires level ${Constants.longreq[index]} fletching"))
                    return true
                }
                ProductionActionService.queueSelection(
                    client,
                    ProductionRequest(
                        skillId = Skill.FLETCHING.id,
                        productId = Constants.longbow[index],
                        amountPerCycle = 1,
                        primaryItemId = itemUsed,
                        secondaryItemId = otherItem,
                        experiencePerUnit = Constants.longexp[index],
                        animationId = 6685 + index,
                        tickDelay = 2,
                        completionMessage = "You string your ${client.GetItemName(Constants.longbows[index]).lowercase()} into a ${client.GetItemName(Constants.longbow[index]).lowercase()}.",
                    ),
                )
                return true
            }
        }
        return false
    }
}
