package net.dodian.uber.game.runtime.tasking

import net.dodian.uber.game.Server
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.runtime.tasking.set.PawnTaskSet
import net.dodian.uber.game.runtime.tasking.set.WorldTaskSet
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
            playerTaskSet(player)
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
            npcTaskSet(npc)
                ?: PawnTaskSet(npc).also { npc.npcTaskSet = it }
        return taskSet.queue(priority, block)
    }

    @JvmStatic
    fun queueWorld(
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend GameTask.() -> Unit,
    ): TaskHandle = worldTaskSet.queue(priority, block)

    @JvmStatic
    fun cycleWorld() {
        worldTaskSet.cycle()
    }

    @JvmStatic
    fun cyclePlayer(player: Client) {
        playerTaskSet(player)?.cycle()
    }

    @JvmStatic
    fun cycleNpc(npc: Npc) {
        npcTaskSet(npc)?.cycle()
    }

    @JvmStatic
    fun cycle() {
        cycleWorld()
    }

    @JvmStatic
    fun submitReturnValue(player: Client, key: TaskRequestKey<*>, value: Any?) {
        playerTaskSet(player)?.submitReturnValue(key, value)
    }

    @JvmStatic
    fun submitReturnValue(npc: Npc, key: TaskRequestKey<*>, value: Any?) {
        npcTaskSet(npc)?.submitReturnValue(key, value)
    }

    @JvmStatic
    fun terminatePlayerTasks(player: Client) {
        playerTaskSet(player)?.terminateTasks()
        player.playerTaskSet = null
    }

    @JvmStatic
    fun terminateNpcTasks(npc: Npc) {
        npcTaskSet(npc)?.terminateTasks()
        npc.npcTaskSet = null
    }

    @JvmStatic
    fun clear() {
        worldTaskSet.terminateTasks()
        PlayerHandler.playersOnline.values.forEach { player: Client ->
            terminatePlayerTasks(player)
        }
        Server.npcManager?.getNpcs()?.forEach { npc ->
            if (npc != null) {
                terminateNpcTasks(npc)
            }
        }
    }

    private fun isClientTaskBlocked(player: Client): Boolean {
        return DialogueService.hasBlockingDialogue(player)
    }

    @Suppress("UNCHECKED_CAST")
    private fun playerTaskSet(player: Client): PawnTaskSet<Client>? = player.playerTaskSet as? PawnTaskSet<Client>

    @Suppress("UNCHECKED_CAST")
    private fun npcTaskSet(npc: Npc): PawnTaskSet<Npc>? = npc.npcTaskSet as? PawnTaskSet<Npc>
}
