package net.dodian.uber.game.events.widget

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client

/**
 * Fired when a player sends a public chat message.
 * Hook for chat filters, mute enforcement, spam detection, and chat logging.
 */
data class ChatMessageEvent(
    val client: Client,
    val message: String,
) : GameEvent

/**
 * Fired when a player sends a private message to another player.
 * [target] is the username string the sender addressed (player may be offline).
 */
data class PrivateMessageEvent(
    val client: Client,
    val targetName: String,
    val message: String,
) : GameEvent

