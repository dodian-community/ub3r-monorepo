package net.dodian.uber.game.systems.api.content

import net.dodian.uber.game.engine.tasking.TaskHandle
import net.dodian.uber.game.engine.tasking.TaskPriority
import net.dodian.uber.game.engine.tasking.npcTaskCoroutine
import net.dodian.uber.game.engine.tasking.playerTaskCoroutine
import net.dodian.uber.game.engine.tasking.worldTaskCoroutine
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

object ContentScheduling {
    @JvmStatic
    @JvmOverloads
    fun world(
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend ContentScheduleScope.() -> Unit,
    ): TaskHandle =
        worldTaskCoroutine(priority = priority) {
            ContentScheduleScope(
                delayTicks = { ticks -> delay(ticks) },
                repeatEvery = { intervalTicks, initialDelayTicks, loop ->
                    repeatEvery(intervalTicks = intervalTicks, initialDelayTicks = initialDelayTicks) {
                        loop()
                    }
                },
            ).block()
        }

    @JvmStatic
    @JvmOverloads
    fun player(
        player: Client,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend ContentScheduleScope.() -> Unit,
    ): TaskHandle =
        playerTaskCoroutine(player = player, priority = priority) {
            ContentScheduleScope(
                delayTicks = { ticks -> delay(ticks) },
                repeatEvery = { intervalTicks, initialDelayTicks, loop ->
                    repeatEvery(intervalTicks = intervalTicks, initialDelayTicks = initialDelayTicks) {
                        loop()
                    }
                },
            ).block()
        }

    @JvmStatic
    @JvmOverloads
    fun npc(
        npc: Npc,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend ContentScheduleScope.() -> Unit,
    ): TaskHandle =
        npcTaskCoroutine(npc = npc, priority = priority) {
            ContentScheduleScope(
                delayTicks = { ticks -> delay(ticks) },
                repeatEvery = { intervalTicks, initialDelayTicks, loop ->
                    repeatEvery(intervalTicks = intervalTicks, initialDelayTicks = initialDelayTicks) {
                        loop()
                    }
                },
            ).block()
        }

    @JvmStatic
    @JvmOverloads
    fun worldRepeating(
        intervalTicks: Int,
        initialDelayTicks: Int = 0,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend () -> Boolean,
    ): TaskHandle =
        world(priority) {
            repeatEvery(intervalTicks = intervalTicks, initialDelayTicks = initialDelayTicks) {
                block()
            }
        }
}

class ContentScheduleScope internal constructor(
    private val delayTicks: suspend (Int) -> Unit,
    private val repeatEvery: suspend (Int, Int, suspend () -> Boolean) -> Unit,
) {
    suspend fun delayTicks(ticks: Int) {
        require(ticks >= 0) { "delay ticks must be >= 0." }
        if (ticks == 0) {
            return
        }
        delayTicks.invoke(ticks)
    }

    suspend fun repeatEvery(
        intervalTicks: Int,
        initialDelayTicks: Int = 0,
        block: suspend () -> Boolean,
    ) {
        require(intervalTicks > 0) { "repeatEvery interval must be > 0." }
        require(initialDelayTicks >= 0) { "repeatEvery initialDelayTicks must be >= 0." }
        repeatEvery.invoke(intervalTicks, initialDelayTicks, block)
    }
}
