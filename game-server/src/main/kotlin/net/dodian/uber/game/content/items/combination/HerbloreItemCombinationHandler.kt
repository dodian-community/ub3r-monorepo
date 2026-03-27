package net.dodian.uber.game.content.items.combination

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.api.content.ContentActions
import net.dodian.uber.game.systems.api.content.ContentProductionMode
import net.dodian.uber.game.systems.api.content.ContentProductionRequest
import net.dodian.uber.game.content.skills.herblore.HerbloreDefinitions

object HerbloreItemCombinationHandler {
    @JvmStatic
    fun handle(client: Client, itemUsed: Int, otherItem: Int): Boolean {
        for (definition in HerbloreDefinitions.herbDefinitions) {
            val herb = definition.cleanId
            if ((itemUsed == herb && otherItem == HerbloreDefinitions.UNFINISHED_POTION_VIAL_ID) ||
                (itemUsed == HerbloreDefinitions.UNFINISHED_POTION_VIAL_ID && otherItem == herb)
            ) {
                if (definition.premiumOnly && !client.premium) {
                    client.send(SendMessage("Need premium to mix these pots!"))
                    return true
                }
                if (client.getSkillLevel(Skill.HERBLORE) < definition.requiredLevel) {
                    client.send(SendMessage("Requires herblore level ${definition.requiredLevel}"))
                    return true
                }
                val xp = if (client.premium || !definition.premiumOnly) definition.cleaningExperience else 0
                ContentActions.queueProductionSelection(
                    client,
                    ContentProductionRequest(
                        skillId = Skill.HERBLORE.id,
                        productId = definition.unfinishedPotionId,
                        amountPerCycle = 1,
                        primaryItemId = itemUsed,
                        secondaryItemId = otherItem,
                        experiencePerUnit = xp,
                        animationId = 363,
                        tickDelay = 1,
                        completionMessage = "You mix the ${client.GetItemName(if (itemUsed != 227) itemUsed else otherItem).lowercase()} herb with the vial of water.",
                    ),
                )
                return true
            }
        }

        for (recipe in HerbloreDefinitions.potionRecipes) {
            if ((itemUsed == recipe.secondaryId && otherItem == recipe.unfinishedPotionId) ||
                (itemUsed == recipe.unfinishedPotionId && otherItem == recipe.secondaryId)
            ) {
                if (recipe.premiumOnly && !client.premium) {
                    client.send(SendMessage("Need premium to mix these pots!"))
                    return true
                }
                if (client.getLevel(Skill.HERBLORE) < recipe.requiredLevel) {
                    client.send(SendMessage("Requires herblore level ${recipe.requiredLevel}"))
                    return true
                }
                ContentActions.queueProductionSelection(
                    client,
                    ContentProductionRequest(
                        skillId = Skill.HERBLORE.id,
                        productId = recipe.finishedPotionId,
                        amountPerCycle = 1,
                        primaryItemId = itemUsed,
                        secondaryItemId = otherItem,
                        experiencePerUnit = recipe.experience,
                        animationId = 363,
                        tickDelay = 3,
                        completionMessage = "You mix the ${client.GetItemName(recipe.secondaryId)} into your potion.",
                    ),
                )
                return true
            }
        }

        val superCombatItems = intArrayOf(269, 2436, 2440, 2442)
        val usedMatchesSuperCombat = superCombatItems.any { potionItem ->
            (potionItem == 269 && (itemUsed == 111 || itemUsed == potionItem)) || itemUsed == potionItem
        }
        val otherMatchesSuperCombat = superCombatItems.any { potionItem ->
            (potionItem == 269 && (otherItem == 111 || otherItem == potionItem)) || otherItem == potionItem
        }
        if (usedMatchesSuperCombat && otherMatchesSuperCombat) {
            var hasAllItems = true
            for (required in superCombatItems) {
                if (required == 269) {
                    if (!client.playerHasItem(269) && !client.playerHasItem(111)) {
                        hasAllItems = false
                    }
                } else if (!client.playerHasItem(required)) {
                    hasAllItems = false
                }
            }
            if (!hasAllItems) {
                client.send(SendMessage("You need a torstol herb or (unf) potion, super attack, strength and defence potion!"))
                return true
            }
            if (client.getSkillLevel(Skill.HERBLORE) < 88) {
                client.send(SendMessage("You need level 88 herblore to mix a super combat potion!"))
                return true
            }
            ContentActions.queueProductionSelection(
                client,
                ContentProductionRequest(
                    skillId = Skill.HERBLORE.id,
                    productId = 12695,
                    amountPerCycle = 1,
                    primaryItemId = 2436,
                    secondaryItemId = -1,
                    experiencePerUnit = 600,
                    animationId = 363,
                    tickDelay = 3,
                    completionMessage = "You mix the ingredients together and made a super combat potion.",
                    mode = ContentProductionMode.SUPER_COMBAT,
                ),
            )
            return true
        }

        val overloadItems = intArrayOf(12695, 2444, 5978)
        val usedMatchesOverload = overloadItems.any { itemUsed == it }
        val otherMatchesOverload = overloadItems.any { otherItem == it }
        if (usedMatchesOverload && otherMatchesOverload) {
            val hasAllItems = overloadItems.all { client.playerHasItem(it) }
            if (!hasAllItems) {
                client.send(SendMessage("You need a coconut, super combat potion and a ranging potion!"))
                return true
            }
            if (client.getSkillLevel(Skill.HERBLORE) < 93) {
                client.send(SendMessage("You need level 93 herblore to mix an overload potion!"))
                return true
            }
            ContentActions.queueProductionSelection(
                client,
                ContentProductionRequest(
                    skillId = Skill.HERBLORE.id,
                    productId = 11730,
                    amountPerCycle = 1,
                    primaryItemId = 5978,
                    secondaryItemId = -1,
                    experiencePerUnit = 800,
                    animationId = 363,
                    tickDelay = 3,
                    completionMessage = "You mix the ingredients together and made an overload potion.",
                    mode = ContentProductionMode.OVERLOAD,
                ),
            )
            return true
        }

        return false
    }

    @JvmStatic
    fun handleDoseMixing(client: Client, itemUsed: Int, useWith: Int): Boolean {
        for (dose in HerbloreDefinitions.potionDoseDefinitions) {
            if ((itemUsed == dose.fourDoseId && useWith == HerbloreDefinitions.EMPTY_VIAL_ID) ||
                (itemUsed == HerbloreDefinitions.EMPTY_VIAL_ID && useWith == dose.fourDoseId)
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(dose.twoDoseId, 1)
                client.addItem(dose.twoDoseId, 1)
                return true
            }
        }
        for (dose in HerbloreDefinitions.potionDoseDefinitions) {
            if ((itemUsed == dose.threeDoseId && useWith == dose.threeDoseId) ||
                (itemUsed == dose.threeDoseId && useWith == dose.threeDoseId)
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(dose.fourDoseId, 1)
                client.addItem(dose.twoDoseId, 1)
                return true
            } else if ((itemUsed == dose.threeDoseId && useWith == dose.twoDoseId) ||
                (itemUsed == dose.twoDoseId && useWith == dose.threeDoseId)
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(dose.fourDoseId, 1)
                client.addItem(dose.oneDoseId, 1)
                return true
            }
        }
        for (dose in HerbloreDefinitions.potionDoseDefinitions) {
            if ((itemUsed == dose.twoDoseId && useWith == HerbloreDefinitions.EMPTY_VIAL_ID) ||
                (itemUsed == HerbloreDefinitions.EMPTY_VIAL_ID && useWith == dose.twoDoseId)
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(dose.oneDoseId, 1)
                client.addItem(dose.oneDoseId, 1)
                return true
            } else if ((itemUsed == dose.twoDoseId && useWith == dose.twoDoseId) ||
                (itemUsed == dose.twoDoseId && useWith == dose.twoDoseId)
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(dose.fourDoseId, 1)
                client.addItem(HerbloreDefinitions.EMPTY_VIAL_ID, 1)
                return true
            }
        }
        for (dose in HerbloreDefinitions.potionDoseDefinitions) {
            if ((itemUsed == dose.oneDoseId && useWith == dose.oneDoseId) ||
                (itemUsed == dose.oneDoseId && useWith == dose.oneDoseId)
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(dose.twoDoseId, 1)
                client.addItem(HerbloreDefinitions.EMPTY_VIAL_ID, 1)
                return true
            } else if ((itemUsed == dose.oneDoseId && useWith == dose.twoDoseId) ||
                (itemUsed == dose.twoDoseId && useWith == dose.oneDoseId)
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(dose.threeDoseId, 1)
                client.addItem(HerbloreDefinitions.EMPTY_VIAL_ID, 1)
                return true
            } else if ((itemUsed == dose.oneDoseId && useWith == dose.threeDoseId) ||
                (itemUsed == dose.threeDoseId && useWith == dose.oneDoseId)
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(dose.fourDoseId, 1)
                client.addItem(HerbloreDefinitions.EMPTY_VIAL_ID, 1)
                return true
            }
        }
        return false
    }
}
