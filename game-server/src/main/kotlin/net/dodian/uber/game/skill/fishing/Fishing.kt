package net.dodian.uber.game.skill.fishing

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.api.content.ContentTiming
import net.dodian.uber.game.skill.runtime.action.ActionStopReason
import net.dodian.uber.game.skill.runtime.action.CycleSignal
import net.dodian.uber.game.skill.runtime.action.RunningGatheringAction
import net.dodian.uber.game.skill.runtime.action.SkillingRandomEventService
import net.dodian.uber.game.skill.runtime.action.gatheringAction
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.engine.util.Misc
import java.util.Collections
import java.util.WeakHashMap

object Fishing {
    private const val REAPPLY_ANIMATION_DELAY_MS = 1800L
    private val fishingTasks = Collections.synchronizedMap(WeakHashMap<Client, RunningGatheringAction>())

    @JvmStatic
    fun cycleDelayMs(client: Client): Long {
        val level = client.getLevel(Skill.FISHING) / 256.0
        val harpoon =
            client.getLevel(Skill.FISHING) >= 61 &&
                (client.equipment[Equipment.Slot.WEAPON.id] == 21028 || client.playerHasItem(21028))
        val bonus = 1 + level + (if (harpoon) 0.2 else 0.0)
        val fishIndex = client.fishingState?.spotIndex ?: return 0L
        val spot = FishingData.byIndex(fishIndex) ?: return 0L
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
        val spot = FishingData.findSpot(objectId, click)
        if (spot == null) {
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(-1)) {
            client.sendMessage("Not enough inventory space.")
            client.resetAction(true)
            return
        }
        if (client.getLevel(Skill.FISHING) < spot.requiredLevel) {
            client.sendMessage("You need level ${spot.requiredLevel} fishing to fish here.")
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(spot.toolItemId) && !harpoon) {
            client.sendMessage("You need a ${client.getItemName(spot.toolItemId).lowercase()} to fish here.")
            client.resetAction(true)
            return
        }
        if (spot.premiumOnly && !client.premium) {
            client.sendMessage("You need to be premium to fish from this spot!")
            client.resetAction(true)
            return
        }
        if (spot.featherConsumed && !client.playerHasItem(314)) {
            client.sendMessage("You do not have any feathers.")
            client.resetAction(true)
            return
        }
        client.fishingState = FishingState(spot.index, 0)
        client.performAnimation(spot.animationId, 0)
        client.sendMessage("You start fishing...")
        startAction(client)
    }

    @JvmStatic
    fun startAction(client: Client) {
        stopAction(client, ActionStopReason.USER_INTERRUPT)
        var nextCatchCycle = ContentTiming.currentCycle() + GameCycleClock.ticksForDurationMs(cycleDelayMs(client))
        var nextAnimationCycle = ContentTiming.currentCycle() + GameCycleClock.ticksForDurationMs(REAPPLY_ANIMATION_DELAY_MS)

        val action =
            gatheringAction("fishing") {
                delay(1)
                onCycleSignal {
                    if (fishingState == null) {
                        return@onCycleSignal CycleSignal.stop()
                    }
                    val cycle = ContentTiming.currentCycle()
                    if (cycle >= nextAnimationCycle) {
                        val fishIndex = fishingState?.spotIndex ?: return@onCycleSignal CycleSignal.stop()
                        val spot = FishingData.byIndex(fishIndex)
                        if (spot != null) {
                            performAnimation(spot.animationId, 0)
                        }
                        nextAnimationCycle = cycle + GameCycleClock.ticksForDurationMs(REAPPLY_ANIMATION_DELAY_MS)
                    }
                    if (cycle < nextCatchCycle) {
                        return@onCycleSignal CycleSignal.continueWithoutSuccess()
                    }
                    performCycle(this)
                    if (fishingState == null) {
                        return@onCycleSignal CycleSignal.stop()
                    }
                    nextCatchCycle = cycle + GameCycleClock.ticksForDurationMs(cycleDelayMs(this))
                    nextAnimationCycle = cycle + GameCycleClock.ticksForDurationMs(REAPPLY_ANIMATION_DELAY_MS)
                    CycleSignal.success()
                }
                onStop {
                    fishingTasks.remove(this)
                    clearFishingState()
                    lastFishAction = 0
                }
            }
        val running = action.start(client) ?: run {
            client.clearFishingState()
            client.lastFishAction = 0
            return
        }
        fishingTasks[client] = running
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun stopFromReset(client: Client, fullReset: Boolean) {
        stopAction(client, ActionStopReason.USER_INTERRUPT)
    }

    @JvmStatic
    fun stopAction(
        client: Client,
        reason: ActionStopReason = ActionStopReason.USER_INTERRUPT,
    ) {
        fishingTasks.remove(client)?.cancel(reason)
    }

    @JvmStatic
    fun performCycle(client: Client) {
        val state = client.fishingState ?: run {
            client.resetAction(true)
            return
        }
        val fishIndex = state.spotIndex
        val spot = FishingData.byIndex(fishIndex) ?: run {
            client.resetAction(true)
            return
        }
        val harpoon = client.getLevel(Skill.FISHING) >= 61 &&
            (client.equipment[Equipment.Slot.WEAPON.id] == 21028 || client.playerHasItem(21028))
        if (!client.playerHasItem(spot.toolItemId) && !harpoon) {
            client.sendMessage("You need a ${client.getItemName(spot.toolItemId).lowercase()} to fish here.")
            client.resetAction(true)
            return
        }
        if (!client.playerHasItem(-1)) {
            client.sendMessage("Not enough inventory space.")
            client.resetAction(true)
            return
        }
        if (spot.featherConsumed && !client.playerHasItem(314)) {
            client.sendMessage("You do not have any feathers.")
            client.resetAction(true)
            return
        }

        val random = Misc.random(6)
        val itemId = if (fishIndex == 1 && client.getLevel(Skill.FISHING) >= 30 && random < 3) 331 else spot.fishItemId
        if (spot.featherConsumed) client.deleteItem(314, 1)
        ProgressionService.addXp(client, if (itemId == 331) spot.experience + 100 else spot.experience, Skill.FISHING)
        client.addItem(itemId, 1)
        client.checkItemUpdate()
        ItemLog.playerGathering(client, itemId, 1, client.position.copy(), "Fishing")
        val gatheredCount = state.gatheredCount + 1
        client.performAnimation(spot.animationId, 0)
        SkillingRandomEventService.trigger(client, spot.experience)
        client.sendMessage("You fish up some ${client.getItemName(itemId).lowercase().replace("raw ", "")}.")
        client.fishingState = state.copy(gatheredCount = gatheredCount)
        if (gatheredCount >= 4 && Misc.chance(20) == 1) {
            client.sendMessage("You take a rest after gathering ${client.resourcesGathered} resources.")
            client.resetAction(true)
        }
    }

    @JvmStatic
    fun attempt(client: Client, objectId: Int, clickOption: Int) = start(client, objectId, clickOption)

    @JvmStatic
    fun handleNpcOption(client: Client, npcId: Int, option: Int): Boolean {
        if (option != 1 && option != 2) {
            return false
        }
        if (FishingData.findSpot(npcId, option) == null) {
            return false
        }
        attempt(client, npcId, option)
        return true
    }
}

object FishingSkillPlugin : SkillPlugin {
    override val definition =
        skillPlugin(name = "Fishing", skill = Skill.FISHING) {
            val byOption = FishingData.fishingSpots.groupBy { it.clickType }
            val firstNpcIds = byOption[1].orEmpty().map { it.objectId }.distinct().toIntArray()
            val secondNpcIds = byOption[2].orEmpty().map { it.objectId }.distinct().toIntArray()

            if (firstNpcIds.isNotEmpty()) {
                npcClick(preset = PolicyPreset.GATHERING, option = 1, *firstNpcIds) { client, npc ->
                    Fishing.attempt(client, npc.id, 1)
                    true
                }
            }
            if (secondNpcIds.isNotEmpty()) {
                npcClick(preset = PolicyPreset.GATHERING, option = 2, *secondNpcIds) { client, npc ->
                    Fishing.attempt(client, npc.id, 2)
                    true
                }
            }
        }
}
