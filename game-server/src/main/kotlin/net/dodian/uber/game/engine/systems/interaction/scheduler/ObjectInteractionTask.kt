package net.dodian.uber.game.engine.systems.interaction.scheduler

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.interaction.InteractionIntent

class ObjectInteractionTask(
    player: Client,
    intent: InteractionIntent,
) : InteractionQueueTask(player, intent, InteractionRoutePolicy.OBJECT)
