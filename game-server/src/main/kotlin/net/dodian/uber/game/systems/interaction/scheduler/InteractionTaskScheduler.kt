package net.dodian.uber.game.systems.interaction.scheduler

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.interaction.InteractionIntent
import net.dodian.uber.game.systems.interaction.GroundItemInteractionIntent
import net.dodian.uber.game.systems.interaction.ItemOnNpcIntent
import net.dodian.uber.game.systems.interaction.ItemOnObjectIntent
import net.dodian.uber.game.systems.interaction.MagicOnNpcIntent
import net.dodian.uber.game.systems.interaction.MagicOnObjectIntent
import net.dodian.uber.game.systems.interaction.MagicOnPlayerIntent
import net.dodian.uber.game.systems.interaction.NpcInteractionIntent
import net.dodian.uber.game.systems.interaction.ObjectClickIntent
import net.dodian.uber.game.systems.interaction.PlayerInteractionIntent
import net.dodian.uber.game.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.scheduler.QueueTaskHandle
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.engine.tasking.TaskPriority

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
            is ItemOnNpcIntent,
            is MagicOnNpcIntent -> PlayerActionCancelReason.NPC_INTERACTION
            is MagicOnPlayerIntent -> PlayerActionCancelReason.PLAYER_INTERACTION
            is NpcInteractionIntent ->
                if (intent.option == 5) {
                    PlayerActionCancelReason.NEW_ACTION
                } else {
                    PlayerActionCancelReason.NPC_INTERACTION
                }
            is PlayerInteractionIntent -> PlayerActionCancelReason.PLAYER_INTERACTION
            is GroundItemInteractionIntent -> PlayerActionCancelReason.GROUND_ITEM_INTERACTION
            else -> PlayerActionCancelReason.NEW_ACTION
        }
}
