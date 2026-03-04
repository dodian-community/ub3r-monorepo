package net.dodian.uber.game.runtime.interaction.task

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.interaction.InteractionIntent

class NpcInteractionTask(
    player: Client,
    intent: InteractionIntent,
) : InteractionQueueTask(player, intent, InteractionRoutePolicy.NPC)
