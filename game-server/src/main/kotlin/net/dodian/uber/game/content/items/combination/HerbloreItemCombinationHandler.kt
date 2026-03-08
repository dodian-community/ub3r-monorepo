package net.dodian.uber.game.content.items.combination

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Utils

object HerbloreItemCombinationHandler {
    @JvmStatic
    fun handle(client: Client, itemUsed: Int, otherItem: Int): Boolean {
        for (index in Utils.herbs.indices) {
            val herb = Utils.herbs[index]
            if ((itemUsed == herb && otherItem == 227) || (itemUsed == 227 && otherItem == herb)) {
                if (!client.premium && index > 2) {
                    client.send(SendMessage("Need premium to mix these pots!"))
                    return true
                }
                if (client.getSkillLevel(Skill.HERBLORE) < Utils.grimy_herbs_lvl[index]) {
                    client.send(SendMessage("Requires herblore level ${Utils.grimy_herbs_lvl[index]}"))
                    return true
                }
                val xp = if (client.premium) Utils.grimy_herbs_xp[index] else 0
                client.setSkillAction(Skill.HERBLORE.id, Utils.herb_unf[index], 1, itemUsed, otherItem, xp, 363, 1)
                client.skillMessage =
                    "You mix the ${client.GetItemName(if (itemUsed != 227) itemUsed else otherItem).lowercase()} herb with the vial of water."
                return true
            }
        }

        for (index in Utils.unf_potion.indices) {
            if ((itemUsed == Utils.secondary[index] && otherItem == Utils.unf_potion[index]) ||
                (itemUsed == Utils.unf_potion[index] && otherItem == Utils.secondary[index])
            ) {
                if (!client.premium && index > 3) {
                    client.send(SendMessage("Need premium to mix these pots!"))
                    return true
                }
                if (client.getLevel(Skill.HERBLORE) < Utils.req[index]) {
                    client.send(SendMessage("Requires herblore level ${Utils.req[index]}"))
                    return true
                }
                client.setSkillAction(
                    Skill.HERBLORE.id,
                    Utils.finished[index],
                    1,
                    itemUsed,
                    otherItem,
                    Utils.potexp[index],
                    363,
                    3,
                )
                client.skillMessage = "You mix the ${client.GetItemName(Utils.secondary[index])} into your potion."
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
            client.setSkillAction(Skill.HERBLORE.id, 12695, 1, 2436, -1, 600, 363, 3)
            client.skillMessage = "You mix the ingredients together and made a super combat potion."
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
            client.setSkillAction(Skill.HERBLORE.id, 11730, 1, 5978, -1, 800, 363, 3)
            client.skillMessage = "You mix the ingredients together and made an overload potion."
            return true
        }

        return false
    }

    @JvmStatic
    fun handleDoseMixing(client: Client, itemUsed: Int, useWith: Int): Boolean {
        for (index in Utils.pot_4_dose.indices) {
            if ((itemUsed == Utils.pot_4_dose[index] && useWith == 229) || (itemUsed == 229 && useWith == Utils.pot_4_dose[index])) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(Utils.pot_2_dose[index], 1)
                client.addItem(Utils.pot_2_dose[index], 1)
                return true
            }
        }
        for (index in Utils.pot_3_dose.indices) {
            if ((itemUsed == Utils.pot_3_dose[index] && useWith == Utils.pot_3_dose[index]) ||
                (itemUsed == Utils.pot_3_dose[index] && useWith == Utils.pot_3_dose[index])
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(Utils.pot_4_dose[index], 1)
                client.addItem(Utils.pot_2_dose[index], 1)
                return true
            } else if ((itemUsed == Utils.pot_3_dose[index] && useWith == Utils.pot_2_dose[index]) ||
                (itemUsed == Utils.pot_2_dose[index] && useWith == Utils.pot_3_dose[index])
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(Utils.pot_4_dose[index], 1)
                client.addItem(Utils.pot_1_dose[index], 1)
                return true
            }
        }
        for (index in Utils.pot_2_dose.indices) {
            if ((itemUsed == Utils.pot_2_dose[index] && useWith == 229) || (itemUsed == 229 && useWith == Utils.pot_2_dose[index])) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(Utils.pot_1_dose[index], 1)
                client.addItem(Utils.pot_1_dose[index], 1)
                return true
            } else if ((itemUsed == Utils.pot_2_dose[index] && useWith == Utils.pot_2_dose[index]) ||
                (itemUsed == Utils.pot_2_dose[index] && useWith == Utils.pot_2_dose[index])
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(Utils.pot_4_dose[index], 1)
                client.addItem(229, 1)
                return true
            }
        }
        for (index in Utils.pot_1_dose.indices) {
            if ((itemUsed == Utils.pot_1_dose[index] && useWith == Utils.pot_1_dose[index]) ||
                (itemUsed == Utils.pot_1_dose[index] && useWith == Utils.pot_1_dose[index])
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(Utils.pot_2_dose[index], 1)
                client.addItem(229, 1)
                return true
            } else if ((itemUsed == Utils.pot_1_dose[index] && useWith == Utils.pot_2_dose[index]) ||
                (itemUsed == Utils.pot_2_dose[index] && useWith == Utils.pot_1_dose[index])
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(Utils.pot_3_dose[index], 1)
                client.addItem(229, 1)
                return true
            } else if ((itemUsed == Utils.pot_1_dose[index] && useWith == Utils.pot_3_dose[index]) ||
                (itemUsed == Utils.pot_3_dose[index] && useWith == Utils.pot_1_dose[index])
            ) {
                client.deleteItem(itemUsed, 1)
                client.deleteItem(useWith, 1)
                client.addItem(Utils.pot_4_dose[index], 1)
                client.addItem(229, 1)
                return true
            }
        }
        return false
    }
}
