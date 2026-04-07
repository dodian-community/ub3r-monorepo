package net.dodian.uber.game.events.item

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player examines an item (right-click -> Examine). */
@Suppress("unused")
data class ItemExamineEvent(
    val client: Client,
    val itemId: Int,
    val contextValue: Int,
) : GameEvent
