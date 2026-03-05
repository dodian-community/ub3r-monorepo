package net.dodian.uber.game.skills.core

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill

object LegacyProductionAdapter {
    @JvmStatic
    fun executeLegacySkillAction(client: Client): Boolean {
        if (client.skillActionCount < 1) {
            client.resetAction()
            return true
        }
        if (client.isBusy) {
            client.sendFilterMessage("You are currently busy to be doing this!")
            return true
        }
        if (client.playerSkillAction.size < 8) {
            client.resetAction()
            return true
        }

        val itemOne = client.playerSkillAction[3]
        val itemTwo = client.playerSkillAction[4]
        val itemMake = client.playerSkillAction[1]
        var amount = client.playerSkillAction[2]

        if (itemMake == 12695) {
            if (!client.playerHasItem(itemOne) || (!client.playerHasItem(111) && !client.playerHasItem(269)) || !client.playerHasItem(2440) || !client.playerHasItem(2442)) {
                client.resetAction()
                val text =
                    if (!client.playerHasItem(111) && !client.playerHasItem(269)) client.GetItemName(269).lowercase()
                    else if (!client.playerHasItem(itemOne)) client.GetItemName(2436).lowercase()
                    else if (!client.playerHasItem(2440)) client.GetItemName(2440).lowercase()
                    else client.GetItemName(2442).lowercase()
                client.sendFilterMessage("You do not have anymore $text.")
                return true
            }
            client.deleteItem(itemOne, amount)
            client.deleteItem(if (!client.playerHasItem(269)) 111 else 269, amount)
            client.deleteItem(2440, amount)
            client.deleteItem(2442, amount)
            client.addItem(itemMake, amount)
        } else if (itemMake == 11730) {
            if (!client.playerHasItem(itemOne) || !client.playerHasItem(2444) || !client.playerHasItem(12695)) {
                client.resetAction()
                val text =
                    if (!client.playerHasItem(itemOne)) client.GetItemName(itemOne).lowercase()
                    else if (!client.playerHasItem(2444)) client.GetItemName(2444).lowercase()
                    else client.GetItemName(12695).lowercase()
                client.sendFilterMessage("You do not have anymore $text.")
                return true
            }
            client.deleteItem(itemOne, amount)
            client.deleteItem(2444, amount)
            client.deleteItem(12695, amount)
            client.addItem(itemMake, amount)
        } else if (itemMake in 569..576) {
            if (!client.playerHasItem(itemOne) || !client.playerHasItem(itemTwo, 3)) {
                client.resetAction()
                client.sendFilterMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk.")
                return true
            }
            client.callGfxMask(if (itemMake == 569) 152 else 149 + ((itemMake - 571) / 2), 100)
            client.deleteItem(itemOne, amount)
            client.deleteRunes(intArrayOf(itemTwo), intArrayOf(3))
            client.addItem(itemMake, amount)
        } else if (itemMake == 1775) {
            if (!client.playerHasItem(itemOne) || !client.playerHasItem(itemTwo)) {
                client.resetAction()
                client.sendFilterMessage("You need one bucket of sand and one soda ash")
                return true
            }
            client.deleteItem(itemOne, amount)
            client.addItem(1925, amount)
            client.deleteItem(itemTwo, amount)
            client.addItem(itemMake, amount)
        } else {
            if (!client.playerHasItem(itemOne) || (itemTwo != -1 && !client.playerHasItem(itemTwo))) {
                client.resetAction()
                val missingName = if (!client.playerHasItem(itemOne)) client.GetItemName(itemOne).lowercase() else client.GetItemName(itemTwo).lowercase()
                client.sendFilterMessage("You do not have anymore $missingName.")
                return true
            }
            if (client.getInvAmt(itemOne) < amount || (itemTwo != -1 && client.getInvAmt(itemTwo) < amount)) {
                amount = if (itemTwo == -1) client.getInvAmt(itemOne) else minOf(client.getInvAmt(itemOne), client.getInvAmt(itemTwo))
            }
            val spec =
                ProductionSpec(
                    actionName = "LegacyProduction",
                    skillId = client.playerSkillAction[0],
                    productId = itemMake,
                    amountPerCycle = amount,
                    primaryItemId = itemOne,
                    secondaryItemId = itemTwo,
                    experiencePerUnit = client.playerSkillAction[5],
                    animationId = client.playerSkillAction[6],
                    tickDelay = client.playerSkillAction[7],
                )
            if (!LegacySkillActionProductionTask(client, spec).runCycle()) {
                client.resetAction()
                return true
            }
        }

        client.checkItemUpdate()
        if (client.playerSkillAction[6] != -1) {
            client.requestAnim(client.playerSkillAction[6], 0)
        }
        val xp = client.playerSkillAction[5] * amount
        client.giveExperience(xp, Skill.getSkill(client.playerSkillAction[0]))
        client.triggerRandom(xp)
        client.skillActionCount--
        client.skillActionTimer = client.playerSkillAction[7]
        if (client.skillMessage.isNotEmpty()) {
            client.sendFilterMessage(client.skillMessage)
        }
        return true
    }

    private class LegacySkillActionProductionTask(
        client: Client,
        spec: ProductionSpec,
    ) : ProductionTask(client, spec) {
        override fun performCycle(): Boolean {
            if (spec.amountPerCycle <= 0) {
                return false
            }
            client.deleteItem(spec.primaryItemId, spec.amountPerCycle)
            if (spec.secondaryItemId != -1) {
                client.deleteItem(spec.secondaryItemId, spec.amountPerCycle)
            }
            client.addItem(spec.productId, spec.amountPerCycle)
            return true
        }
    }
}
