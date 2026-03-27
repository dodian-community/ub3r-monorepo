package net.dodian.uber.game.skills.fishing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.skills.core.runtime.SkillingRandomEventService
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.runtime.action.SkillingActionService
import net.dodian.utilities.Misc

object FishingService {
    @JvmStatic
    fun cycleDelayMs(client: Client): Long {
        val level = client.getLevel(Skill.FISHING) / 256.0
        val harpoon =
            client.getLevel(Skill.FISHING) >= 61 &&
                (client.equipment[Equipment.Slot.WEAPON.id] == 21028 || client.playerHasItem(21028))
        val bonus = 1 + level + (if (harpoon) 0.2 else 0.0)
        val fishIndex = client.fishingState?.spotIndex ?: return 0L
        val spot = FishingDefinitions.byIndex(fishIndex) ?: return 0L
        var timer = spot.baseDelayMs.toDouble()
        val chance = Misc.chance(8) == 1
        if (chance && harpoon) {
            timer -= 600
        }
        return (timer / bonus).toLong()
    }

    @JvmStatic
    fun start(client: Client, objectId: Int, click: Int) {
        val harpoon = client.getLevel(Skill.FISHING) >= 61 &&
            (client.equipment[Equipment.Slot.WEAPON.id] == 21028 || client.playerHasItem(21028))
        val spot = FishingDefinitions.findSpot(objectId, click)
        if (spot == null) {
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(-1)) {
            client.send(SendMessage("Not enough inventory space."))
            client.resetAction(true)
            return
        }
        if (client.getLevel(Skill.FISHING) < spot.requiredLevel) {
            client.send(SendMessage("You need level ${spot.requiredLevel} fishing to fish here."))
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(spot.toolItemId) && !harpoon) {
            client.send(SendMessage("You need a ${client.GetItemName(spot.toolItemId).lowercase()} to fish here."))
            client.resetAction(true)
            return
        }
        if (spot.premiumOnly && !client.premium) {
            client.send(SendMessage("You need to be premium to fish from this spot!"))
            client.resetAction(true)
            return
        }
        if (spot.featherConsumed && !client.playerHasItem(314)) {
            client.send(SendMessage("You do not have any feathers."))
            client.resetAction(true)
            return
        }
        client.fishingState = FishingState(spot.index, 0)
        client.requestAnim(spot.animationId, 0)
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
        val spot = FishingDefinitions.byIndex(fishIndex) ?: run {
            client.resetAction(true)
            return
        }
        val harpoon = client.getLevel(Skill.FISHING) >= 61 &&
            (client.equipment[Equipment.Slot.WEAPON.id] == 21028 || client.playerHasItem(21028))
        if (!client.playerHasItem(spot.toolItemId) && !harpoon) {
            client.send(SendMessage("You need a ${client.GetItemName(spot.toolItemId).lowercase()} to fish here."))
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(-1)) {
            client.send(SendMessage("Not enough inventory space."))
            client.resetAction(true)
            return
        }
        if (spot.featherConsumed && !client.playerHasItem(314)) {
            client.send(SendMessage("You do not have any feathers."))
            client.resetAction(true)
            return
        }

        val random = Misc.random(6)
        val itemId = if (fishIndex == 1 && client.getLevel(Skill.FISHING) >= 30 && random < 3) 331 else spot.fishItemId
        if (spot.featherConsumed) client.deleteItem(314, 1)
        SkillProgressionService.gainXp(client, if (itemId == 331) spot.experience + 100 else spot.experience, Skill.FISHING)
        client.addItem(itemId, 1)
        client.checkItemUpdate()
        ItemLog.playerGathering(client, itemId, 1, client.position.copy(), "Fishing")
        val gatheredCount = state.gatheredCount + 1
        client.requestAnim(spot.animationId, 0)
        SkillingRandomEventService.trigger(client, spot.experience)
        client.send(SendMessage("You fish up some ${client.GetItemName(itemId).lowercase().replace("raw ", "")}."))
        client.fishingState = state.copy(gatheredCount = gatheredCount)
        if (gatheredCount >= 4 && Misc.chance(20) == 1) {
            client.send(SendMessage("You take a rest after gathering ${client.resourcesGathered} resources."))
            client.resetAction(true)
        }
    }
}
