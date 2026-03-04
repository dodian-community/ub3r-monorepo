package net.dodian.uber.game.runtime.interaction.task

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.runtime.interaction.InteractionIntent
import net.dodian.uber.game.runtime.queue.QueueTaskService

object InteractionTaskScheduler {
    @JvmStatic
    fun schedule(player: Client, intent: InteractionIntent, task: InteractionQueueTask) {
        player.cancelInteractionTask()
        player.pendingInteraction = intent
        player.activeInteraction = null
        player.interactionEarliestCycle = PlayerHandler.cycle + 1L
        player.interactionTaskHandle = QueueTaskService.schedule(1, 1, task)
    }
}
