package net.dodian.uber.game.engine.tasking.coroutine

import kotlinx.coroutines.CancellationException
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.tasking.GameTask
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.engine.tasking.TaskHandle
import net.dodian.uber.game.engine.tasking.TaskPriority

class WorldTaskContext internal constructor(
    private val task: GameTask,
) {
    suspend fun delay(ticks: Int) {
        require(ticks >= 0) { "delay ticks must be >= 0." }
        if (ticks == 0) return
        task.wait(ticks)
    }

    suspend fun stop(): Nothing {
        task.terminate()
        throw CancellationException("World task stopped")
    }

    suspend fun repeatEvery(
        intervalTicks: Int,
        initialDelayTicks: Int = 0,
        block: suspend WorldTaskContext.() -> Boolean,
    ) {
        require(intervalTicks > 0) { "repeatEvery interval must be > 0." }
        require(initialDelayTicks >= 0) { "repeatEvery initialDelayTicks must be >= 0." }
        if (initialDelayTicks > 0) {
            delay(initialDelayTicks)
        }
        while (block()) {
            delay(intervalTicks)
        }
    }
}

class PlayerTaskContext internal constructor(
    val player: Client,
    private val task: GameTask,
) {
    suspend fun delay(ticks: Int) {
        require(ticks >= 0) { "delay ticks must be >= 0." }
        if (ticks == 0) return
        task.wait(ticks)
    }

    suspend fun stop(): Nothing {
        task.terminate()
        throw CancellationException("Player task stopped")
    }

    suspend fun repeatEvery(
        intervalTicks: Int,
        initialDelayTicks: Int = 0,
        block: suspend PlayerTaskContext.() -> Boolean,
    ) {
        require(intervalTicks > 0) { "repeatEvery interval must be > 0." }
        require(initialDelayTicks >= 0) { "repeatEvery initialDelayTicks must be >= 0." }
        if (initialDelayTicks > 0) {
            delay(initialDelayTicks)
        }
        while (block()) {
            delay(intervalTicks)
        }
    }
}

class NpcTaskContext internal constructor(
    val npc: Npc,
    private val task: GameTask,
) {
    suspend fun delay(ticks: Int) {
        require(ticks >= 0) { "delay ticks must be >= 0." }
        if (ticks == 0) return
        task.wait(ticks)
    }

    suspend fun stop(): Nothing {
        task.terminate()
        throw CancellationException("Npc task stopped")
    }

    suspend fun repeatEvery(
        intervalTicks: Int,
        initialDelayTicks: Int = 0,
        block: suspend NpcTaskContext.() -> Boolean,
    ) {
        require(intervalTicks > 0) { "repeatEvery interval must be > 0." }
        require(initialDelayTicks >= 0) { "repeatEvery initialDelayTicks must be >= 0." }
        if (initialDelayTicks > 0) {
            delay(initialDelayTicks)
        }
        while (block()) {
            delay(intervalTicks)
        }
    }
}

fun gameClock(): Long = GameCycleClock.currentCycle()

fun worldTaskCoroutine(
    priority: TaskPriority = TaskPriority.STANDARD,
    block: suspend WorldTaskContext.() -> Unit,
): TaskHandle =
    GameTaskRuntime.queueWorld(priority) {
        val context = WorldTaskContext(this)
        try {
            block(context)
        } catch (_: CancellationException) {
            // Expected stop/cancel path for world task coroutines.
        }
    }

fun playerTaskCoroutine(
    player: Client,
    priority: TaskPriority = TaskPriority.STANDARD,
    block: suspend PlayerTaskContext.() -> Unit,
): TaskHandle =
    GameTaskRuntime.queuePlayer(player, priority) {
        val context = PlayerTaskContext(player, this)
        try {
            block(context)
        } catch (_: CancellationException) {
            // Expected stop/cancel path for player task coroutines.
        }
    }

fun npcTaskCoroutine(
    npc: Npc,
    priority: TaskPriority = TaskPriority.STANDARD,
    block: suspend NpcTaskContext.() -> Unit,
): TaskHandle =
    GameTaskRuntime.queueNpc(npc, priority) {
        val context = NpcTaskContext(npc, this)
        try {
            block(context)
        } catch (_: CancellationException) {
            // Expected stop/cancel path for npc task coroutines.
        }
    }

fun runWorldTaskCoroutine(
    priority: TaskPriority = TaskPriority.STANDARD,
    block: suspend WorldTaskContext.() -> Unit,
): TaskHandle = worldTaskCoroutine(priority, block)

fun runPlayerTaskCoroutine(
    player: Client,
    priority: TaskPriority = TaskPriority.STANDARD,
    block: suspend PlayerTaskContext.() -> Unit,
): TaskHandle = playerTaskCoroutine(player, priority, block)

fun runNpcTaskCoroutine(
    npc: Npc,
    priority: TaskPriority = TaskPriority.STANDARD,
    block: suspend NpcTaskContext.() -> Unit,
): TaskHandle = npcTaskCoroutine(npc, priority, block)
