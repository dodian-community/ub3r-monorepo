package net.dodian.uber.game.systems.action

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.smithing.SmeltingActionService
import net.dodian.uber.game.content.skills.fletching.FletchingService
import net.dodian.uber.game.content.skills.crafting.CraftingService
import net.dodian.uber.game.content.skills.crafting.CraftingMode
import net.dodian.uber.game.content.skills.cooking.CookingService
import net.dodian.uber.game.content.skills.fishing.FishingDefinitions
import net.dodian.uber.game.content.skills.fishing.FishingService
import net.dodian.uber.game.content.skills.prayer.PrayerInteractionService
import net.dodian.uber.game.systems.skills.ActionStopReason
import net.dodian.uber.game.systems.skills.CycleSignal
import net.dodian.uber.game.systems.skills.RunningGatheringAction
import net.dodian.uber.game.systems.skills.RunningProductionAction
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.systems.api.content.ContentTiming
import net.dodian.uber.game.systems.skills.gatheringAction
import net.dodian.uber.game.systems.skills.productionAction
import java.util.Collections
import java.util.WeakHashMap
import net.dodian.uber.game.systems.action.dsl.playerAction

object SkillingActionService {
    private const val STANDARD_ACTION_DELAY_MS = 1800L
    private const val REAPPLY_ANIMATION_DELAY_MS = 1800L
    private val fletchingTasks = Collections.synchronizedMap(WeakHashMap<Client, RunningProductionAction>())
    private val fishingTasks = Collections.synchronizedMap(WeakHashMap<Client, RunningGatheringAction>())
    private val cookingTasks = Collections.synchronizedMap(WeakHashMap<Client, RunningProductionAction>())

    @JvmStatic
    fun startSmelting(client: Client) {
        SmeltingActionService.start(client)
    }

    @JvmStatic
    fun startShafting(client: Client) {
        playerAction(
            player = client,
            type = PlayerActionType.SHAFTING,
            actionName = "shafting",
            onStop = { player, _ ->
                player.clearCraftingState()
            },
        ) {
            while (player.craftingState?.mode == CraftingMode.SHAFTING) {
                CraftingService.performShaft(player)
                emitCycleSuccess("shafting")
                if (player.craftingState?.mode != CraftingMode.SHAFTING) return@playerAction
                waitTicks(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
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
        playerAction(
            player = client,
            type = PlayerActionType.SPINNING,
            actionName = "spinning",
            onStop = { player, _ ->
                player.clearCraftingState()
            },
        ) {
            while (player.craftingState?.mode == CraftingMode.SPINNING) {
                CraftingService.performSpin(player)
                emitCycleSuccess("spinning")
                if (player.craftingState?.mode != CraftingMode.SPINNING) return@playerAction
                waitTicks(GameCycleClock.ticksForDurationMs(CraftingService.spinDelayMs(player)))
            }
        }
    }

    @JvmStatic
    fun startCrafting(client: Client) {
        playerAction(
            player = client,
            type = PlayerActionType.CRAFTING,
            actionName = "crafting",
            onStop = { player, _ ->
                player.clearCraftingState()
            },
        ) {
            while ((player.craftingState?.remaining ?: 0) > 0 && player.craftingState?.mode == CraftingMode.LEATHER) {
                CraftingService.performCraft(player)
                emitCycleSuccess("crafting")
                if ((player.craftingState?.remaining ?: 0) <= 0 || player.craftingState?.mode != CraftingMode.LEATHER) return@playerAction
                waitTicks(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
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
                            performAnimation(spot.animationId, 0)
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
        playerAction(
            player = client,
            type = PlayerActionType.ALTAR_BONES,
            actionName = "altar_bones",
            onStop = { player, _ ->
                player.clearPrayerOfferingState()
            },
        ) {
            while (player.prayerOfferingState != null) {
                val boneItemId = player.prayerOfferingState?.boneItemId ?: return@playerAction
                emitCycle("altar_bones")
                if (!PrayerInteractionService.altarBones(player, boneItemId)) return@playerAction
                emitSuccess("altar_bones")
                waitTicks(3)
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
