package net.dodian.uber.game.systems.interaction.scheduler

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.entities.npcs.spawns.NpcClickMetrics
import net.dodian.uber.game.systems.interaction.InteractionIntent
import net.dodian.uber.game.systems.interaction.InteractionProcessor
import net.dodian.uber.game.systems.interaction.NpcInteractionIntent
import net.dodian.uber.game.engine.scheduler.QueueTask

abstract class InteractionQueueTask(
    protected val player: Client,
    protected val intent: InteractionIntent,
    val routePolicy: InteractionRoutePolicy,
) : QueueTask {
    override fun execute(): Boolean {
        if (player.pendingInteraction !== intent) {
            if (intent is NpcInteractionIntent) {
                NpcClickMetrics.recordQueueStale(player.playerName, intent::class.java.simpleName)
            }
            return false
        }
        return InteractionProcessor.process(player) == InteractionExecutionResult.WAITING
    }
}
