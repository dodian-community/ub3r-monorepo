package net.dodian.uber.game.systems.api.content

import net.dodian.uber.game.engine.tasking.TaskHandle
import net.dodian.uber.game.engine.tasking.TaskPriority
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.skills.farming.runtime.FarmingRuntimeService

/**
 * Content-facing runtime API entrypoint.
 *
 * Runtime internals under `runtime.*` are implementation details and can change as
 * the engine evolves. Content modules should prefer this package for common operations
 * (actions, interaction throttles/policies, timing, and safety checks).
 *
 * This keeps content code focused on gameplay behavior while allowing internal runtime
 * subsystems to be reorganized with minimal churn.
 */
object ContentRuntimeApi {
    @JvmStatic
    fun nowMs(): Long = System.currentTimeMillis()

    @JvmStatic
    @JvmOverloads
    fun farmingWorldLoop(
        intervalTicks: Int = 1,
        initialDelayTicks: Int = 0,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend () -> Boolean,
    ): TaskHandle =
        ContentScheduling.world(priority = priority) {
            repeatEvery(intervalTicks = intervalTicks, initialDelayTicks = initialDelayTicks) {
                block()
            }
        }

    @JvmStatic
    @JvmOverloads
    fun farmingPlayerLoop(
        player: Client,
        intervalTicks: Int = 1,
        initialDelayTicks: Int = 0,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend () -> Boolean,
    ): TaskHandle =
        ContentScheduling.player(player = player, priority = priority) {
            repeatEvery(intervalTicks = intervalTicks, initialDelayTicks = initialDelayTicks) {
                block()
            }
        }

    @JvmStatic
    @JvmOverloads
    fun onFarmingLogin(player: Client, nowMs: Long = nowMs()) {
        val loginCycle = ContentTiming.currentCycle()
        farmingPlayerLoop(player = player, initialDelayTicks = 1) {
            if (player.disconnected || !player.isActive) {
                return@farmingPlayerLoop false
            }
            FarmingRuntimeService.INSTANCE.recordDeferredLoginCatchUpStart(player, loginCycle)
            FarmingRuntimeService.INSTANCE.onLogin(player, nowMs)
            false
        }
    }

    @JvmStatic
    @JvmOverloads
    fun onFarmingPatchInteraction(player: Client, nowMs: Long = nowMs()) {
        FarmingRuntimeService.INSTANCE.onPatchInteraction(player, nowMs)
    }

    @JvmStatic
    @JvmOverloads
    fun onFarmingCompostInteraction(player: Client, nowMs: Long = nowMs()) {
        FarmingRuntimeService.INSTANCE.onCompostInteraction(player, nowMs)
    }

    @JvmStatic
    @JvmOverloads
    fun onFarmingSaplingInventoryChange(player: Client, nowMs: Long = nowMs()) {
        FarmingRuntimeService.INSTANCE.onSaplingInventoryChange(player, nowMs)
    }

    @JvmStatic
    @JvmOverloads
    fun onFarmingStateDirty(player: Client, nowMs: Long = nowMs()) {
        FarmingRuntimeService.INSTANCE.onStateDirty(player, nowMs)
    }
}
