package net.dodian.uber.game.tasks

import net.dodian.uber.game.engine.tasking.TaskHandle
import net.dodian.uber.game.engine.tasking.TaskPriority
import net.dodian.uber.game.engine.tasking.NpcTaskContext as EngineNpcTaskContext
import net.dodian.uber.game.engine.tasking.PlayerTaskContext as EnginePlayerTaskContext
import net.dodian.uber.game.engine.tasking.WorldTaskContext as EngineWorldTaskContext
import net.dodian.uber.game.engine.tasking.gameClock as engineGameClock
import net.dodian.uber.game.engine.tasking.npcTaskCoroutine as engineNpcTaskCoroutine
import net.dodian.uber.game.engine.tasking.playerTaskCoroutine as enginePlayerTaskCoroutine
import net.dodian.uber.game.engine.tasking.worldTaskCoroutine as engineWorldTaskCoroutine
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

typealias WorldTaskContext = EngineWorldTaskContext
typealias PlayerTaskContext = EnginePlayerTaskContext
typealias NpcTaskContext = EngineNpcTaskContext

/**
 * Legacy compatibility facade for older task coroutine call sites.
 * Prefer [net.dodian.uber.game.systems.api.content.ContentScheduling] in content code.
 */
@Deprecated(
    message = "Use ContentScheduling instead of TickTasks",
    replaceWith = ReplaceWith("ContentScheduling"),
)
object TickTasks {
    @JvmStatic
    fun gameClock(): Long = engineGameClock()

    @JvmStatic
    @JvmOverloads
    fun worldTaskCoroutine(
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend WorldTaskContext.() -> Unit,
    ): TaskHandle = engineWorldTaskCoroutine(priority = priority, block = block)

    @JvmStatic
    @JvmOverloads
    fun playerTaskCoroutine(
        player: Client,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend PlayerTaskContext.() -> Unit,
    ): TaskHandle = enginePlayerTaskCoroutine(player = player, priority = priority, block = block)

    @JvmStatic
    @JvmOverloads
    fun npcTaskCoroutine(
        npc: Npc,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend NpcTaskContext.() -> Unit,
    ): TaskHandle = engineNpcTaskCoroutine(npc = npc, priority = priority, block = block)
}
