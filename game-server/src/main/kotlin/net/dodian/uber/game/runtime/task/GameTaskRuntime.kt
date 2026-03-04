package net.dodian.uber.game.runtime.task

import net.dodian.uber.game.Server
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.runtime.task.set.PawnTaskSet
import net.dodian.uber.game.runtime.task.set.WorldTaskSet
import java.util.function.Supplier

object GameTaskRuntime {
    private val worldTaskSet = WorldTaskSet()

    @JvmStatic
    fun queuePlayer(
        player: Client,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend GameTask.() -> Unit,
    ): TaskHandle {
        val taskSet =
            (player.playerTaskSet as? PawnTaskSet<Client>)
                ?: PawnTaskSet(player, ::isClientTaskBlocked).also { player.playerTaskSet = it }
        return taskSet.queue(priority, block)
    }

    @JvmStatic
    fun queuePlayerRepeating(
        player: Client,
        priority: TaskPriority = TaskPriority.STANDARD,
        delayTicks: Int = 0,
        intervalTicks: Int = 1,
        step: Supplier<Boolean>,
    ): TaskHandle {
        return queuePlayer(player, priority) {
            val initialDelay = delayTicks.coerceAtLeast(0)
            if (initialDelay > 0) {
                wait(initialDelay)
            }
            val repeating = intervalTicks.coerceAtLeast(0)
            while (true) {
                val keepRunning = step.get()
                if (!keepRunning || repeating <= 0) {
                    return@queuePlayer
                }
                wait(repeating)
            }
        }
    }

    @JvmStatic
    fun queueNpc(
        npc: Npc,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend GameTask.() -> Unit,
    ): TaskHandle {
        val taskSet =
            (npc.npcTaskSet as? PawnTaskSet<Npc>)
                ?: PawnTaskSet(npc).also { npc.npcTaskSet = it }
        return taskSet.queue(priority, block)
    }

    @JvmStatic
    fun queueWorld(
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend GameTask.() -> Unit,
    ): TaskHandle = worldTaskSet.queue(priority, block)

    @JvmStatic
    fun cycle() {
        worldTaskSet.cycle()
        PlayerHandler.snapshotActivePlayers().forEach { player ->
            (player.playerTaskSet as? PawnTaskSet<Client>)?.cycle()
        }
        Server.npcManager?.getNpcs()?.forEach { npc ->
            if (npc != null) {
                (npc.npcTaskSet as? PawnTaskSet<Npc>)?.cycle()
            }
        }
    }

    @JvmStatic
    fun submitReturnValue(player: Client, key: TaskRequestKey<*>, value: Any?) {
        (player.playerTaskSet as? PawnTaskSet<Client>)?.submitReturnValue(key, value)
    }

    @JvmStatic
    fun submitReturnValue(npc: Npc, key: TaskRequestKey<*>, value: Any?) {
        (npc.npcTaskSet as? PawnTaskSet<Npc>)?.submitReturnValue(key, value)
    }

    @JvmStatic
    fun terminatePlayerTasks(player: Client) {
        (player.playerTaskSet as? PawnTaskSet<Client>)?.terminateTasks()
        player.playerTaskSet = null
    }

    @JvmStatic
    fun terminateNpcTasks(npc: Npc) {
        (npc.npcTaskSet as? PawnTaskSet<Npc>)?.terminateTasks()
        npc.npcTaskSet = null
    }

    @JvmStatic
    fun clear() {
        worldTaskSet.terminateTasks()
        PlayerHandler.playersOnline.values.forEach { player ->
            terminatePlayerTasks(player)
        }
        Server.npcManager?.getNpcs()?.forEach { npc ->
            if (npc != null) {
                terminateNpcTasks(npc)
            }
        }
    }

    private fun isClientTaskBlocked(player: Client): Boolean {
        return DialogueService.hasActiveSession(player) ||
            player.NpcDialogue != 0 ||
            player.NpcDialogueSend ||
            player.nextDiag > 0
    }
}
