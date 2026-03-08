package net.dodian.uber.game.runtime.interaction.scheduler

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.loop.GameCycleClock
import net.dodian.uber.game.runtime.interaction.InteractionIntent
import net.dodian.uber.game.runtime.scheduler.QueueTaskHandle
import net.dodian.uber.game.runtime.tasking.GameTaskRuntime
import net.dodian.uber.game.runtime.tasking.TaskPriority

object InteractionTaskScheduler {
    @JvmStatic
    fun schedule(player: Client, intent: InteractionIntent, task: InteractionQueueTask) {
        player.cancelInteractionTask()
        player.pendingInteraction = intent
        player.activeInteraction = null
        player.interactionEarliestCycle = GameCycleClock.currentCycle()
        val handle =
            GameTaskRuntime.queuePlayer(player, TaskPriority.STANDARD) {
                while (task.execute()) {
                    wait(1)
                }
            }
        player.interactionTaskHandle = QueueTaskHandle.from(handle)
    }
}
