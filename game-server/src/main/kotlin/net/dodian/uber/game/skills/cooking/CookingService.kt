package net.dodian.uber.game.skills.cooking

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.runtime.action.SkillingActionService
import net.dodian.utilities.Utils

object CookingService {
    @JvmStatic
    fun start(client: Client, itemId: Int) {
        if (client.isBusy) {
            client.send(SendMessage("You are currently busy to be cooking!"))
            return
        }
        var valid = false
        for (i in Utils.cookIds.indices) {
            if (itemId == Utils.cookIds[i]) {
                client.setCookIndex(i)
                valid = true
                break
            }
        }
        if (!valid) {
            return
        }
        client.setCookAmount(client.getInvAmt(itemId))
        client.cooking = true
        SkillingActionService.startCooking(client)
    }

    @JvmStatic
    fun performCycle(client: Client) {
        if (client.isBusy || client.getCookAmount() < 1) {
            client.resetAction(true)
            return
        }
        val cookIndex = client.getCookIndex()
        val itemId = Utils.cookIds[cookIndex]
        if (!client.playerHasItem(itemId)) {
            client.send(SendMessage("You are out of fish"))
            client.resetAction(true)
            return
        }
        if (client.getLevel(Skill.COOKING) < Utils.cookLevel[cookIndex]) {
            client.send(SendMessage("You need ${Utils.cookLevel[cookIndex]} cooking to cook the ${Server.itemManager.getName(itemId).lowercase()}."))
            client.resetAction(true)
            return
        }

        var ran = when (itemId) {
            2134, 317 -> 30 - client.getLevel(Skill.COOKING)
            2138, 2307 -> 36 - client.getLevel(Skill.COOKING)
            3363 -> 42 - client.getLevel(Skill.COOKING)
            335 -> 50 - client.getLevel(Skill.COOKING)
            331 -> 60 - client.getLevel(Skill.COOKING)
            377 -> 70 - client.getLevel(Skill.COOKING)
            371 -> 80 - client.getLevel(Skill.COOKING)
            7944 -> 90 - client.getLevel(Skill.COOKING)
            383 -> 100 - client.getLevel(Skill.COOKING)
            395 -> 110 - client.getLevel(Skill.COOKING)
            389 -> 120 - client.getLevel(Skill.COOKING)
            else -> 0
        }
        if (client.equipment[Equipment.Slot.HANDS.id] == 775) ran -= 4
        if (client.equipment[Equipment.Slot.HEAD.id] == 1949) ran -= 4
        if (client.equipment[Equipment.Slot.HEAD.id] == 1949 && client.equipment[Equipment.Slot.HANDS.id] == 775) ran -= 2
        ran = ran.coerceIn(0, 100)
        val burn = 1 + Utils.random(99) <= ran

        if (Utils.cookExp[cookIndex] <= 0) {
            client.resetAction(true)
            return
        }
        client.setCookAmount(client.getCookAmount() - 1)
        client.deleteItem(itemId, 1)
        client.setFocus(client.skillX, client.skillY)
        client.requestAnim(883, 0)
        if (!burn) {
            client.addItem(Utils.cookedIds[cookIndex], 1)
            client.send(SendMessage("You cook the ${client.GetItemName(itemId)}"))
            client.giveExperience(Utils.cookExp[cookIndex], Skill.COOKING)
        } else {
            client.addItem(Utils.burnId[cookIndex], 1)
            client.send(SendMessage("You burn the ${client.GetItemName(itemId)}"))
        }
        client.checkItemUpdate()
        client.triggerRandom(Utils.cookExp[cookIndex])
    }
}
