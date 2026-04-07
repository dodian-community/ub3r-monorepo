package net.dodian.uber.game.events.player

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.player.PlayerSaveReason

/** Fired when a player logs out or disconnects from the world. */
data class PlayerLogoutEvent(
    val client: Client,
    val reason: PlayerSaveReason,
) : GameEvent
