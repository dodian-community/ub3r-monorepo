package net.dodian.uber.game.runtime.interaction.scheduler

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.InteractionIntent
import net.dodian.uber.game.runtime.interaction.GroundItemInteractionIntent
import net.dodian.uber.game.runtime.interaction.ItemOnObjectIntent
import net.dodian.uber.game.runtime.interaction.MagicOnObjectIntent
import net.dodian.uber.game.runtime.interaction.NpcInteractionIntent
import net.dodian.uber.game.runtime.interaction.ObjectClickIntent
import net.dodian.uber.game.runtime.interaction.PlayerInteractionIntent
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason
import net.dodian.uber.game.runtime.loop.GameCycleClock
import net.dodian.uber.game.runtime.scheduler.QueueTaskHandle
import net.dodian.uber.game.runtime.tasking.GameTaskRuntime
import net.dodian.uber.game.runtime.tasking.TaskPriority

object InteractionTaskScheduler {
    @JvmStatic
    fun schedule(player: Client, intent: InteractionIntent, task: InteractionQueueTask) {
        player.cancelInteractionTask()
        PlayerActionCancellationService.cancel(
            player = player,
            reason = cancelReason(intent),
            fullResetAnimation = false,
            resetCompatibilityState = true,
        )
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

    private fun cancelReason(intent: InteractionIntent): PlayerActionCancelReason =
        when (intent) {
            is ObjectClickIntent,
            is ItemOnObjectIntent,
            is MagicOnObjectIntent -> PlayerActionCancelReason.OBJECT_INTERACTION
            is NpcInteractionIntent -> PlayerActionCancelReason.NPC_INTERACTION
            is PlayerInteractionIntent -> PlayerActionCancelReason.PLAYER_INTERACTION
            is GroundItemInteractionIntent -> PlayerActionCancelReason.GROUND_ITEM_INTERACTION
            else -> PlayerActionCancelReason.NEW_ACTION
        }
}
