package net.dodian.uber.game.runtime.action

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.prayer.Prayer
import net.dodian.uber.game.runtime.loop.GameCycleClock
import net.dodian.uber.game.skills.core.LegacyProductionAdapter
import net.dodian.utilities.Utils

object LegacyPlayerActionService {
    private const val STANDARD_ACTION_DELAY_MS = 1800L
    private const val REAPPLY_ANIMATION_DELAY_MS = 1800L

    @JvmStatic
    fun startSmelting(client: Client) {
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.SMELTING,
            onStop = { it.smelting = false },
        ) {
            while (player.smelting && player.smeltCount > 0) {
                if (!isActive()) {
                    return@start
                }
                player.smelt(player.smelt_id)
                if (!player.smelting || player.smeltCount <= 0) {
                    return@start
                }
                wait(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
    }

    @JvmStatic
    fun startGoldCrafting(client: Client) {
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.GOLD_CRAFTING,
            onStop = { it.goldCrafting = false },
        ) {
            while (player.goldCrafting && player.goldCraftingCount > 0) {
                if (!isActive()) {
                    return@start
                }
                player.goldCraft()
                if (!player.goldCrafting || player.goldCraftingCount <= 0) {
                    return@start
                }
                wait(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
    }

    @JvmStatic
    fun startShafting(client: Client) {
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.SHAFTING,
            onStop = { it.shafting = false },
        ) {
            while (player.shafting) {
                if (!isActive()) {
                    return@start
                }
                player.shaft()
                if (!player.shafting) {
                    return@start
                }
                wait(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
    }

    @JvmStatic
    fun startFletching(client: Client) {
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.FLETCHING,
            onStop = { it.fletchings = false },
        ) {
            while (player.fletchings && player.fletchAmount > 0) {
                if (!isActive()) {
                    return@start
                }
                player.fletching.fletchBow(player)
                if (!player.fletchings || player.fletchAmount <= 0) {
                    return@start
                }
                wait(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
    }

    @JvmStatic
    fun startSpinning(client: Client) {
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.SPINNING,
            onStop = { it.spinning = false },
        ) {
            while (player.spinning) {
                if (!isActive()) {
                    return@start
                }
                player.spin()
                if (!player.spinning) {
                    return@start
                }
                wait(GameCycleClock.ticksForDurationMs(player.spinSpeed))
            }
        }
    }

    @JvmStatic
    fun startCrafting(client: Client) {
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.CRAFTING,
            onStop = { it.setCrafting(false) },
        ) {
            while (player.isCrafting() && player.getCAmount() > 0) {
                if (!isActive()) {
                    return@start
                }
                player.craft()
                if (!player.isCrafting() || player.getCAmount() <= 0) {
                    return@start
                }
                wait(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
    }

    @JvmStatic
    fun startFishing(client: Client) {
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.FISHING,
            onStop = {
                it.setFishing(false)
                it.lastFishAction = 0
            },
        ) {
            var nextCatchCycle = currentCycle() + GameCycleClock.ticksForDurationMs(player.fishingSpeed)
            var nextAnimationCycle = currentCycle() + GameCycleClock.ticksForDurationMs(REAPPLY_ANIMATION_DELAY_MS)
            while (player.isFishing()) {
                if (!isActive()) {
                    return@start
                }
                val cycle = currentCycle()
                if (cycle >= nextAnimationCycle) {
                    val fishIndex = player.fishIndex
                    if (fishIndex >= 0 && fishIndex < Utils.fishAnim.size) {
                        player.requestAnim(Utils.fishAnim[fishIndex], 0)
                    }
                    nextAnimationCycle = cycle + GameCycleClock.ticksForDurationMs(REAPPLY_ANIMATION_DELAY_MS)
                }
                if (cycle >= nextCatchCycle) {
                    player.fish()
                    if (!player.isFishing()) {
                        return@start
                    }
                    nextCatchCycle = cycle + GameCycleClock.ticksForDurationMs(player.fishingSpeed)
                    nextAnimationCycle = cycle + GameCycleClock.ticksForDurationMs(REAPPLY_ANIMATION_DELAY_MS)
                }
                wait(1)
            }
        }
    }

    @JvmStatic
    fun startCooking(client: Client) {
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.COOKING,
            onStop = { it.cooking = false },
        ) {
            while (player.cooking && player.cookAmount > 0) {
                if (!isActive()) {
                    return@start
                }
                player.cook()
                if (!player.cooking || player.cookAmount <= 0) {
                    return@start
                }
                wait(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
    }

    @JvmStatic
    fun startLegacyProduction(client: Client) {
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.LEGACY_PRODUCTION,
            onStop = { it.skillActionTimer = -1 },
        ) {
            while (player.skillActionCount > 0 && player.playerSkillAction.size >= 8) {
                if (!isActive()) {
                    return@start
                }
                LegacyProductionAdapter.executeLegacySkillAction(player)
                if (player.skillActionCount <= 0 || player.playerSkillAction.size < 8) {
                    return@start
                }
                wait(player.playerSkillAction[7].coerceAtLeast(1))
            }
        }
    }

    @JvmStatic
    fun startAltarBones(client: Client) {
        PlayerActionController.start(
            player = client,
            type = PlayerActionType.ALTAR_BONES,
            onStop = {
                it.prayerAction = -1
                it.boneItem = -1
            },
        ) {
            while (player.boneItem > 0) {
                if (!isActive()) {
                    return@start
                }
                if (!Prayer.altarBones(player, player.boneItem)) {
                    return@start
                }
                wait(3)
            }
        }
    }
}
