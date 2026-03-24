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
        var selectedIndex = -1
        for (i in Utils.fishSpots.indices) {
            if (Utils.fishSpots[i] != objectId) continue
            if (click == 1 && (i == 0 || i == 2 || i == 4 || i == 6)) {
                valid = true
                selectedIndex = i
                break
            }
            if (click == 2 && (i == 1 || i == 3 || i == 5 || i == 7)) {
                valid = true
                selectedIndex = i
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
        if (client.getLevel(Skill.FISHING) < Utils.fishReq[selectedIndex]) {
            client.send(SendMessage("You need level ${Utils.fishReq[selectedIndex]} fishing to fish here."))
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(Utils.fishTool[selectedIndex]) && !harpoon) {
            client.send(SendMessage("You need a ${client.GetItemName(Utils.fishTool[selectedIndex]).lowercase()} to fish here."))
            client.resetAction(true)
            return
        }
        if ((selectedIndex == 4 || selectedIndex >= 6) && !client.premium) {
            client.send(SendMessage("You need to be premium to fish from this spot!"))
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(314) && selectedIndex == 1) {
            client.send(SendMessage("You do not have any feathers."))
            client.resetAction(true)
            return
        }
        client.fishingState = FishingState(selectedIndex, 0)
        client.requestAnim(Utils.fishAnim[selectedIndex], 0)
        client.send(SendMessage("You start fishing..."))
        SkillingActionService.startFishing(client)
    }

    @JvmStatic
    fun performCycle(client: Client) {
        val state = client.fishingState ?: run {
            client.resetAction(true)
            return
        }
        val fishIndex = state.spotIndex
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
        val gatheredCount = state.gatheredCount + 1
        client.requestAnim(Utils.fishAnim[fishIndex], 0)
        client.triggerRandom(Utils.fishExp[fishIndex])
        client.send(SendMessage("You fish up some ${client.GetItemName(itemId).lowercase().replace("raw ", "")}."))
        client.fishingState = state.copy(gatheredCount = gatheredCount)
        if (gatheredCount >= 4 && Misc.chance(20) == 1) {
            client.send(SendMessage("You take a rest after gathering ${client.resourcesGathered} resources."))
            client.resetAction(true)
        }
    }
}
