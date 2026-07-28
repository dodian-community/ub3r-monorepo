package net.dodian.uber.game.engine.systems.interaction.scheduler
import net.dodian.uber.game.api.content.ContentActions

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.interaction.InteractionIntent
import net.dodian.uber.game.engine.systems.interaction.AttackPlayerIntent
import net.dodian.uber.game.engine.systems.interaction.GroundItemInteractionIntent
import net.dodian.uber.game.engine.systems.interaction.ItemOnNpcIntent
import net.dodian.uber.game.engine.systems.interaction.ItemOnObjectIntent
import net.dodian.uber.game.engine.systems.interaction.MagicOnNpcIntent
import net.dodian.uber.game.engine.systems.interaction.MagicOnObjectIntent
import net.dodian.uber.game.engine.systems.interaction.MagicOnPlayerIntent
import net.dodian.uber.game.engine.systems.interaction.NpcInteractionIntent
import net.dodian.uber.game.engine.systems.interaction.ObjectClickIntent
import net.dodian.uber.game.engine.systems.interaction.PlayerInteractionIntent
import net.dodian.uber.game.engine.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.engine.state.InteractionSessionStateAdapter
import net.dodian.uber.game.engine.scheduler.QueueTaskHandle
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.engine.tasking.TaskPriority

object InteractionTaskScheduler {
    @JvmStatic
    fun schedule(player: Client, intent: InteractionIntent, task: InteractionQueueTask) {
        if (serverOwnsApproach(intent)) {
            player.claimServerRoutedInteractionMovement()
        }
        player.cancelInteractionTask()
        ContentActions.cancel(
            player = player,
            reason = cancelReason(intent),
            fullResetAnimation = false,
            resetCompatibilityState = true,
        )
        InteractionSessionStateAdapter.schedule(player, intent)
        player.activeInteraction = null
        player.interactionEarliestCycle = GameCycleClock.currentCycle()

        when (intent) {
            is ObjectClickIntent -> player.setPersistedFaceCoord(intent.objectPosition.x, intent.objectPosition.y)
            is ItemOnObjectIntent -> player.setPersistedFaceCoord(intent.objectPosition.x, intent.objectPosition.y)
            is MagicOnObjectIntent -> player.setPersistedFaceCoord(intent.objectPosition.x, intent.objectPosition.y)
            is NpcInteractionIntent -> {
                val npc = net.dodian.uber.game.Server.npcManager.npcMap[intent.npcIndex]
                if (npc != null) {
                    player.setPersistedFaceCoord(npc.position.x, npc.position.y)
                }
            }
            is ItemOnNpcIntent -> {
                val npc = net.dodian.uber.game.Server.npcManager.npcMap[intent.npcIndex]
                if (npc != null) {
                    player.setPersistedFaceCoord(npc.position.x, npc.position.y)
                }
            }
            is MagicOnNpcIntent -> {
                val npc = net.dodian.uber.game.Server.npcManager.npcMap[intent.npcIndex]
                if (npc != null) {
                    player.setPersistedFaceCoord(npc.position.x, npc.position.y)
                }
            }
            is AttackPlayerIntent -> {
                val target = net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.getClient(intent.victimIndex)
                if (target != null) {
                    player.setPersistedFaceCoord(target.position.x, target.position.y)
                }
            }
            is MagicOnPlayerIntent -> {
                val target = net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.getClient(intent.victimIndex)
                if (target != null) {
                    player.setPersistedFaceCoord(target.position.x, target.position.y)
                }
            }
            is PlayerInteractionIntent -> {
                val target = net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.getClient(intent.playerIndex)
                if (target != null) {
                    player.setPersistedFaceCoord(target.position.x, target.position.y)
                }
            }
            is GroundItemInteractionIntent -> player.setPersistedFaceCoord(intent.x, intent.y)
        }

        val handle =
            GameTaskRuntime.queuePlayer(player, TaskPriority.STANDARD) {
                while (task.execute()) {
                    wait(1)
                }
            }
        player.interactionTaskHandle = QueueTaskHandle.from(handle)
    }

    private fun serverOwnsApproach(intent: InteractionIntent): Boolean =
        when (intent) {
            is ObjectClickIntent,
            is ItemOnObjectIntent,
            is MagicOnObjectIntent,
            is ItemOnNpcIntent,
            is PlayerInteractionIntent -> true
            is NpcInteractionIntent -> intent.option in 1..4
            else -> false
        }

    private fun cancelReason(intent: InteractionIntent): PlayerActionCancelReason =
        when (intent) {
            is ObjectClickIntent,
            is ItemOnObjectIntent,
            is MagicOnObjectIntent -> PlayerActionCancelReason.OBJECT_INTERACTION
            is ItemOnNpcIntent,
            is MagicOnNpcIntent -> PlayerActionCancelReason.NPC_INTERACTION
            is AttackPlayerIntent,
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
