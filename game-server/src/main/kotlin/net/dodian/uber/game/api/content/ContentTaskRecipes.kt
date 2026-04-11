package net.dodian.uber.game.api.content

import net.dodian.uber.game.engine.tasking.TaskHandle
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object ContentTaskRecipes {
    @JvmStatic
    @JvmOverloads
    fun worldCountdown(
        totalTicks: Int,
        onTick: (Int) -> Unit = {},
        onDone: () -> Unit,
    ): TaskHandle {
        require(totalTicks > 0) { "totalTicks must be > 0." }
        return ContentScheduling.world {
            var remaining = totalTicks
            repeatEvery(intervalTicks = 1) {
                onTick(remaining)
                remaining--
                if (remaining <= 0) {
                    onDone()
                    return@repeatEvery false
                }
                true
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun playerCountdown(
        player: Client,
        totalTicks: Int,
        onTick: (Int) -> Unit = {},
        onDone: () -> Unit,
    ): TaskHandle {
        require(totalTicks > 0) { "totalTicks must be > 0." }
        return ContentScheduling.player(player) {
            var remaining = totalTicks
            repeatEvery(intervalTicks = 1) {
                onTick(remaining)
                remaining--
                if (remaining <= 0) {
                    onDone()
                    return@repeatEvery false
                }
                true
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun npcCountdown(
        npc: Npc,
        totalTicks: Int,
        onTick: (Int) -> Unit = {},
        onDone: () -> Unit,
    ): TaskHandle {
        require(totalTicks > 0) { "totalTicks must be > 0." }
        return ContentScheduling.npc(npc) {
            var remaining = totalTicks
            repeatEvery(intervalTicks = 1) {
                onTick(remaining)
                remaining--
                if (remaining <= 0) {
                    onDone()
                    return@repeatEvery false
                }
                true
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun worldRetryUntil(
        intervalTicks: Int,
        initialDelayTicks: Int = 0,
        maxAttempts: Int? = null,
        action: () -> Boolean,
        onGiveUp: () -> Unit = {},
    ): TaskHandle {
        require(intervalTicks > 0) { "intervalTicks must be > 0." }
        require(initialDelayTicks >= 0) { "initialDelayTicks must be >= 0." }
        require(maxAttempts == null || maxAttempts > 0) { "maxAttempts must be > 0 when set." }
        return ContentScheduling.world {
            var attempts = 0
            repeatEvery(intervalTicks = intervalTicks, initialDelayTicks = initialDelayTicks) {
                attempts++
                if (action()) {
                    return@repeatEvery false
                }
                if (maxAttempts != null && attempts >= maxAttempts) {
                    onGiveUp()
                    return@repeatEvery false
                }
                true
            }
        }
    }
}
