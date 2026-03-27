package net.dodian.uber.game.systems.action

import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.event.events.skilling.SkillingActionCycleEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionStartedEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionSucceededEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.smithing.SmeltingActionService
import net.dodian.uber.game.content.skills.fletching.FletchingService
import net.dodian.uber.game.content.skills.crafting.CraftingService
import net.dodian.uber.game.content.skills.crafting.CraftingMode
import net.dodian.uber.game.content.skills.cooking.CookingService
import net.dodian.uber.game.content.skills.fishing.FishingDefinitions
import net.dodian.uber.game.content.skills.fishing.FishingService
import net.dodian.uber.game.content.skills.prayer.PrayerInteractionService
import net.dodian.uber.game.content.skills.core.runtime.ActionStopReason
import net.dodian.uber.game.content.skills.core.runtime.CycleSignal
import net.dodian.uber.game.content.skills.core.runtime.RunningGatheringAction
import net.dodian.uber.game.content.skills.core.runtime.RunningProductionAction
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.systems.api.content.ContentTiming
import net.dodian.uber.game.content.skills.core.events.SkillActionCompleteEvent
import net.dodian.uber.game.content.skills.core.events.SkillActionStartEvent
import net.dodian.uber.game.content.skills.core.runtime.SkillingInterruptService
import net.dodian.uber.game.content.skills.core.runtime.gatheringAction
import net.dodian.uber.game.content.skills.core.runtime.productionAction
import java.util.Collections
import java.util.WeakHashMap

object SkillingActionService {
    private const val STANDARD_ACTION_DELAY_MS = 1800L
    private const val REAPPLY_ANIMATION_DELAY_MS = 1800L
    private val fletchingTasks = Collections.synchronizedMap(WeakHashMap<Client, RunningProductionAction>())
    private val fishingTasks = Collections.synchronizedMap(WeakHashMap<Client, RunningGatheringAction>())
    private val cookingTasks = Collections.synchronizedMap(WeakHashMap<Client, RunningProductionAction>())

    private fun postStarted(player: Client, name: String) {
        GameEventBus.post(SkillingActionStartedEvent(player, name))
        GameEventBus.post(SkillActionStartEvent(player, name))
    }

    private fun postCycle(player: Client, name: String) {
        GameEventBus.post(SkillingActionCycleEvent(player, name))
    }

    private fun postSucceeded(player: Client, name: String) {
        GameEventBus.post(SkillingActionSucceededEvent(player, name))
        GameEventBus.post(SkillActionCompleteEvent(player, name))
    }

    @JvmStatic
    fun startSmelting(client: Client) {
        SmeltingActionService.start(client)
    }

    @JvmStatic
    fun startShafting(client: Client) {
        postStarted(client, "shafting")
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.SHAFTING,
            onStop = { player, result ->
                player.clearCraftingState()
                SkillingInterruptService.postStopped(player, "shafting", (result as? PlayerActionStopResult.Cancelled)?.reason)
            },
        ) {
            while (player.craftingState?.mode == CraftingMode.SHAFTING) {
                if (!isActive()) return@start
                postCycle(player, "shafting")
                CraftingService.performShaft(player)
                postSucceeded(player, "shafting")
                if (player.craftingState?.mode != CraftingMode.SHAFTING) return@start
                wait(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
    }

    @JvmStatic
    fun startFletching(client: Client) {
        stopFletchingTask(client, ActionStopReason.USER_INTERRUPT)
        val action =
            productionAction("fletching") {
                delay { GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS) }
                onCycleWhile {
                    if ((fletchingState?.remaining ?: 0) <= 0) {
                        return@onCycleWhile false
                    }
                    FletchingService.performBowCycle(this)
                    (fletchingState?.remaining ?: 0) > 0
                }
                onStop {
                    fletchingTasks.remove(this)
                    clearFletchingState()
                }
            }
        val running = action.start(client) ?: run {
            client.clearFletchingState()
            return
        }
        fletchingTasks[client] = running
    }

    @JvmStatic
    fun startSpinning(client: Client) {
        postStarted(client, "spinning")
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.SPINNING,
            onStop = { player, result ->
                player.clearCraftingState()
                SkillingInterruptService.postStopped(player, "spinning", (result as? PlayerActionStopResult.Cancelled)?.reason)
            },
        ) {
            while (player.craftingState?.mode == CraftingMode.SPINNING) {
                if (!isActive()) return@start
                postCycle(player, "spinning")
                CraftingService.performSpin(player)
                postSucceeded(player, "spinning")
                if (player.craftingState?.mode != CraftingMode.SPINNING) return@start
                wait(GameCycleClock.ticksForDurationMs(CraftingService.spinDelayMs(player)))
            }
        }
    }

    @JvmStatic
    fun startCrafting(client: Client) {
        postStarted(client, "crafting")
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.CRAFTING,
            onStop = { player, result ->
                player.clearCraftingState()
                SkillingInterruptService.postStopped(player, "crafting", (result as? PlayerActionStopResult.Cancelled)?.reason)
            },
        ) {
            while ((player.craftingState?.remaining ?: 0) > 0 && player.craftingState?.mode == CraftingMode.LEATHER) {
                if (!isActive()) return@start
                postCycle(player, "crafting")
                CraftingService.performCraft(player)
                postSucceeded(player, "crafting")
                if ((player.craftingState?.remaining ?: 0) <= 0 || player.craftingState?.mode != CraftingMode.LEATHER) return@start
                wait(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
    }

    @JvmStatic
    fun startFishing(client: Client) {
        stopFishingTask(client, ActionStopReason.USER_INTERRUPT)
        var nextCatchCycle = ContentTiming.currentCycle() + GameCycleClock.ticksForDurationMs(FishingService.cycleDelayMs(client))
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
                        val spot = FishingDefinitions.byIndex(fishIndex)
                        if (spot != null) {
                            requestAnim(spot.animationId, 0)
                        }
                        nextAnimationCycle = cycle + GameCycleClock.ticksForDurationMs(REAPPLY_ANIMATION_DELAY_MS)
                    }
                    if (cycle < nextCatchCycle) {
                        return@onCycleSignal CycleSignal.continueWithoutSuccess()
                    }
                    FishingService.performCycle(this)
                    if (fishingState == null) {
                        return@onCycleSignal CycleSignal.stop()
                    }
                    nextCatchCycle = cycle + GameCycleClock.ticksForDurationMs(FishingService.cycleDelayMs(this))
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

    @JvmStatic
    fun startCooking(client: Client) {
        stopCookingTask(client, ActionStopReason.USER_INTERRUPT)
        val action =
            productionAction("cooking") {
                delay { GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS) }
                onCycleWhile {
                    if ((cookingState?.remaining ?: 0) <= 0) {
                        return@onCycleWhile false
                    }
                    CookingService.performCycle(this)
                    (cookingState?.remaining ?: 0) > 0
                }
                onStop {
                    cookingTasks.remove(this)
                    clearCookingState()
                }
            }
        val running = action.start(client) ?: run {
            client.clearCookingState()
            return
        }
        cookingTasks[client] = running
    }

    @JvmStatic
    fun startAltarBones(client: Client) {
        postStarted(client, "altar_bones")
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.ALTAR_BONES,
            onStop = { player, result ->
                player.clearPrayerOfferingState()
                SkillingInterruptService.postStopped(player, "altar_bones", (result as? PlayerActionStopResult.Cancelled)?.reason)
            },
        ) {
            while (player.prayerOfferingState != null) {
                if (!isActive()) return@start
                val boneItemId = player.prayerOfferingState?.boneItemId ?: return@start
                postCycle(player, "altar_bones")
                if (!PrayerInteractionService.altarBones(player, boneItemId)) return@start
                postSucceeded(player, "altar_bones")
                wait(3)
            }
        }
    }

    @JvmStatic
    fun stopFletchingFromReset(client: Client, fullReset: Boolean) {
        stopFletchingTask(client, ActionStopReason.USER_INTERRUPT)
    }

    @JvmStatic
    fun stopFishingFromReset(client: Client, fullReset: Boolean) {
        stopFishingTask(client, ActionStopReason.USER_INTERRUPT)
    }

    @JvmStatic
    fun stopCookingFromReset(client: Client, fullReset: Boolean) {
        stopCookingTask(client, ActionStopReason.USER_INTERRUPT)
    }

    private fun stopFletchingTask(client: Client, reason: ActionStopReason) {
        fletchingTasks.remove(client)?.cancel(reason)
    }

    private fun stopFishingTask(client: Client, reason: ActionStopReason) {
        fishingTasks.remove(client)?.cancel(reason)
    }

    private fun stopCookingTask(client: Client, reason: ActionStopReason) {
        cookingTasks.remove(client)?.cancel(reason)
    }
}
