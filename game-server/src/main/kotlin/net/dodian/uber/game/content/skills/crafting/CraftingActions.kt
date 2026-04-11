package net.dodian.uber.game.content.skills.crafting

import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.action.PlayerActionType
import net.dodian.uber.game.engine.systems.action.playerAction

object CraftingActions {
    private const val STANDARD_ACTION_DELAY_MS = 1800L

    @JvmStatic
    fun startShaftingAction(client: Client) {
        playerAction(
            player = client,
            type = PlayerActionType.SHAFTING,
            actionName = "shafting",
            onStop = { player, _ ->
                player.clearCraftingState()
            },
        ) {
            while (player.craftingState?.mode == CraftingMode.SHAFTING) {
                Crafting.performShaft(player)
                emitCycleSuccess("shafting")
                if (player.craftingState?.mode != CraftingMode.SHAFTING) return@playerAction
                waitTicks(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
    }

    @JvmStatic
    fun startSpinningAction(client: Client) {
        playerAction(
            player = client,
            type = PlayerActionType.SPINNING,
            actionName = "spinning",
            onStop = { player, _ ->
                player.clearCraftingState()
            },
        ) {
            while (player.craftingState?.mode == CraftingMode.SPINNING) {
                Crafting.performSpin(player)
                emitCycleSuccess("spinning")
                if (player.craftingState?.mode != CraftingMode.SPINNING) return@playerAction
                waitTicks(GameCycleClock.ticksForDurationMs(Crafting.spinDelayMs(player)))
            }
        }
    }

    @JvmStatic
    fun startCraftingAction(client: Client) {
        playerAction(
            player = client,
            type = PlayerActionType.CRAFTING,
            actionName = "crafting",
            onStop = { player, _ ->
                player.clearCraftingState()
            },
        ) {
            while ((player.craftingState?.remaining ?: 0) > 0 && player.craftingState?.mode == CraftingMode.LEATHER) {
                Crafting.performCraft(player)
                emitCycleSuccess("crafting")
                if ((player.craftingState?.remaining ?: 0) <= 0 || player.craftingState?.mode != CraftingMode.LEATHER) return@playerAction
                waitTicks(GameCycleClock.ticksForDurationMs(STANDARD_ACTION_DELAY_MS))
            }
        }
    }
}
