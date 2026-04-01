package net.dodian.uber.game.events

import net.dodian.uber.game.engine.event.GameEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.player.PlayerSaveReason

data class PlayerLogoutEvent(
    val client: Client,
    val reason: PlayerSaveReason,
) : GameEvent
