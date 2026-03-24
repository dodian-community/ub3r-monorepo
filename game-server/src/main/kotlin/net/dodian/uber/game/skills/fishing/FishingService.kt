package net.dodian.uber.game.skills.fishing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.runtime.action.SkillingActionService
import net.dodian.utilities.Misc
import net.dodian.utilities.Utils

object FishingService {
    @JvmStatic
    fun start(client: Client, objectId: Int, click: Int) {
        var valid = false
        val harpoon = client.getLevel(Skill.FISHING) >= 61 &&
            (client.equipment[Equipment.Slot.WEAPON.id] == 21028 || client.playerHasItem(21028))
        for (i in Utils.fishSpots.indices) {
            if (Utils.fishSpots[i] != objectId) continue
            if (click == 1 && (i == 0 || i == 2 || i == 4 || i == 6)) {
                valid = true
                client.setFishIndex(i)
                break
            }
            if (click == 2 && (i == 1 || i == 3 || i == 5 || i == 7)) {
                valid = true
                client.setFishIndex(i)
                break
            }
        }
        if (!valid) {
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(-1)) {
            client.send(SendMessage("Not enough inventory space."))
            client.resetAction(true)
            return
        }
        if (client.getLevel(Skill.FISHING) < Utils.fishReq[client.getFishIndex()]) {
            client.send(SendMessage("You need level ${Utils.fishReq[client.getFishIndex()]} fishing to fish here."))
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(Utils.fishTool[client.getFishIndex()]) && !harpoon) {
            client.send(SendMessage("You need a ${client.GetItemName(Utils.fishTool[client.getFishIndex()]).lowercase()} to fish here."))
            client.resetAction(true)
            return
        }
        if ((client.getFishIndex() == 4 || client.getFishIndex() >= 6) && !client.premium) {
            client.send(SendMessage("You need to be premium to fish from this spot!"))
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(314) && client.getFishIndex() == 1) {
            client.send(SendMessage("You do not have any feathers."))
            client.resetAction(true)
            return
        }
        client.resourcesGathered = 0
        client.setFishing(true)
        client.requestAnim(Utils.fishAnim[client.getFishIndex()], 0)
        client.send(SendMessage("You start fishing..."))
        SkillingActionService.startFishing(client)
    }

    @JvmStatic
    fun performCycle(client: Client) {
        val fishIndex = client.getFishIndex()
        val harpoon = client.getLevel(Skill.FISHING) >= 61 &&
            (client.equipment[Equipment.Slot.WEAPON.id] == 21028 || client.playerHasItem(21028))
        if (!client.playerHasItem(Utils.fishTool[fishIndex]) && !harpoon) {
            client.send(SendMessage("You need a ${client.GetItemName(Utils.fishTool[fishIndex]).lowercase()} to fish here."))
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(-1)) {
            client.send(SendMessage("Not enough inventory space."))
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(314) && fishIndex == 1) {
            client.send(SendMessage("You do not have any feathers."))
            client.resetAction(true)
            return
        }

        val random = Misc.random(6)
        val itemId = if (fishIndex == 1 && client.getLevel(Skill.FISHING) >= 30 && random < 3) 331 else Utils.fishId[fishIndex]
        if (fishIndex == 1) client.deleteItem(314, 1)
        client.giveExperience(if (itemId == 331) Utils.fishExp[fishIndex] + 100 else Utils.fishExp[fishIndex], Skill.FISHING)
        client.addItem(itemId, 1)
        client.checkItemUpdate()
        ItemLog.playerGathering(client, itemId, 1, client.position.copy(), "Fishing")
        client.resourcesGathered++
        client.requestAnim(Utils.fishAnim[fishIndex], 0)
        client.triggerRandom(Utils.fishExp[fishIndex])
        client.send(SendMessage("You fish up some ${client.GetItemName(itemId).lowercase().replace("raw ", "")}."))
        if (client.resourcesGathered >= 4 && Misc.chance(20) == 1) {
            client.send(SendMessage("You take a rest after gathering ${client.resourcesGathered} resources."))
            client.resetAction(true)
        }
    }
}
