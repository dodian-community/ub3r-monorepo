package net.dodian.uber.game.runtime.action

import net.dodian.uber.game.event.GameEventBus
import net.dodian.uber.game.event.events.skilling.SkillingActionCycleEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionStartedEvent
import net.dodian.uber.game.event.events.skilling.SkillingActionSucceededEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.smithing.SmeltingActionService
import net.dodian.uber.game.skills.fletching.FletchingService
import net.dodian.uber.game.skills.crafting.CraftingService
import net.dodian.uber.game.skills.crafting.CraftingMode
import net.dodian.uber.game.skills.cooking.CookingService
import net.dodian.uber.game.skills.fishing.FishingService
import net.dodian.uber.game.skills.prayer.PrayerInteractionService
import net.dodian.uber.game.runtime.loop.GameCycleClock
import net.dodian.uber.game.skills.core.SkillingInterruptService
import net.dodian.utilities.Utils

object SkillingActionService {
    private const val STANDARD_ACTION_DELAY_MS = 1800L
    private const val REAPPLY_ANIMATION_DELAY_MS = 1800L

    private fun postStarted(player: Client, name: String) {
        GameEventBus.post(SkillingActionStartedEvent(player, name))
    }

    private fun postCycle(player: Client, name: String) {
        GameEventBus.post(SkillingActionCycleEvent(player, name))
    }

    private fun postSucceeded(player: Client, name: String) {
        GameEventBus.post(SkillingActionSucceededEvent(player, name))
    }

    @JvmStatic
    fun startSmelting(client: Client) {
        SmeltingActionService.start(client)
    }

    @JvmStatic
    fun startGoldCrafting(client: Client) {
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.GOLD_CRAFTING,
            onStop = { player, _ -> player.goldCrafting = false },
        ) {
            while (player.goldCrafting && player.goldCraftingCount > 0) {
                if (!isActive()) return@start
                player.goldCraft()
                if (!player.goldCrafting || player.goldCraftingCount <= 0) return@start
                wait(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
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
        postStarted(client, "fletching")
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.FLETCHING,
            onStop = { player, result ->
                player.clearFletchingState()
                SkillingInterruptService.postStopped(player, "fletching", (result as? PlayerActionStopResult.Cancelled)?.reason)
            },
        ) {
            while ((player.fletchingState?.remaining ?: 0) > 0) {
                if (!isActive()) return@start
                postCycle(player, "fletching")
                FletchingService.performBowCycle(player)
                postSucceeded(player, "fletching")
                if ((player.fletchingState?.remaining ?: 0) <= 0) return@start
                wait(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
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
                wait(GameCycleClock.ticksForDurationMs(player.spinSpeed))
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
        postStarted(client, "fishing")
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.FISHING,
            onStop = { player, _ ->
                player.clearFishingState()
                player.lastFishAction = 0
                SkillingInterruptService.postStopped(player, "fishing", player.activeActionCancelReason)
            },
        ) {
            var nextCatchCycle = currentCycle() + GameCycleClock.ticksForDurationMs(player.fishingSpeed)
            var nextAnimationCycle = currentCycle() + GameCycleClock.ticksForDurationMs(REAPPLY_ANIMATION_DELAY_MS)
            while (player.fishingState != null) {
                if (!isActive()) return@start
                val cycle = currentCycle()
                if (cycle >= nextAnimationCycle) {
                    val fishIndex = player.fishingState?.spotIndex ?: return@start
                    if (fishIndex >= 0 && fishIndex < Utils.fishAnim.size) {
                        player.requestAnim(Utils.fishAnim[fishIndex], 0)
                    }
                    nextAnimationCycle = cycle + GameCycleClock.ticksForDurationMs(REAPPLY_ANIMATION_DELAY_MS)
                }
                if (cycle >= nextCatchCycle) {
                    postCycle(player, "fishing")
                    FishingService.performCycle(player)
                    postSucceeded(player, "fishing")
                    if (player.fishingState == null) return@start
                    nextCatchCycle = cycle + GameCycleClock.ticksForDurationMs(player.fishingSpeed)
                    nextAnimationCycle = cycle + GameCycleClock.ticksForDurationMs(REAPPLY_ANIMATION_DELAY_MS)
                }
                wait(1)
            }
        }
    }

    @JvmStatic
    fun startCooking(client: Client) {
        postStarted(client, "cooking")
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.COOKING,
            onStop = { player, result ->
                player.clearCookingState()
                SkillingInterruptService.postStopped(player, "cooking", (result as? PlayerActionStopResult.Cancelled)?.reason)
            },
        ) {
            while ((player.cookingState?.remaining ?: 0) > 0) {
                if (!isActive()) return@start
                postCycle(player, "cooking")
                CookingService.performCycle(player)
                postSucceeded(player, "cooking")
                if ((player.cookingState?.remaining ?: 0) <= 0) return@start
                wait(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
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
}
