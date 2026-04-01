package net.dodian.uber.game.systems.interaction.scheduler

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.interaction.InteractionIntent

class PlayerInteractionTask(
    player: Client,
    intent: InteractionIntent,
) : InteractionQueueTask(player, intent, InteractionRoutePolicy.PLAYER)
