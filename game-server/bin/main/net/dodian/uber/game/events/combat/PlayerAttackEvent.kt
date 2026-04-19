package net.dodian.uber.game.events.combat

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client

/**
 * Fired when a player initiates an attack on another player or NPC.
 * [targetIndex] is the server-side entity index of the target.
 * Use this for PvP zone enforcement, safe-zone blocking, and combat logging.
 */
data class PlayerAttackEvent(
    val attacker: Client,
    val targetIndex: Int,
    val targetIsPlayer: Boolean,
) : GameEvent

