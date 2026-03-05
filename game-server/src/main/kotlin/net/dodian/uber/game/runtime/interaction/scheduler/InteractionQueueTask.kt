package net.dodian.uber.game.runtime.interaction.scheduler

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.InteractionIntent
import net.dodian.uber.game.runtime.interaction.InteractionProcessor
import net.dodian.uber.game.runtime.scheduler.QueueTask

abstract class InteractionQueueTask(
    protected val player: Client,
    protected val intent: InteractionIntent,
    val routePolicy: InteractionRoutePolicy,
) : QueueTask {
    override fun execute(): Boolean {
        if (player.pendingInteraction !== intent) {
            return false
        }
        return InteractionProcessor.process(player) == InteractionExecutionResult.WAITING
    }
}
