package net.dodian.uber.game.engine.systems.interaction.scheduler

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.state.InteractionSessionStateAdapter
import net.dodian.uber.game.engine.systems.interaction.InteractionIntent
import net.dodian.uber.game.engine.systems.interaction.InteractionProcessor
import net.dodian.uber.game.engine.scheduler.QueueTask

abstract class InteractionQueueTask(
    protected val player: Client,
    protected val intent: InteractionIntent,
    val routePolicy: InteractionRoutePolicy,
) : QueueTask {
    override fun execute(): Boolean {
        if (!InteractionSessionStateAdapter.isPending(player, intent)) {
            return false
        }
        return InteractionProcessor.process(player) == InteractionExecutionResult.WAITING
    }
}
