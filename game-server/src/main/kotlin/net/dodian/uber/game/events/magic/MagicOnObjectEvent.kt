package net.dodian.uber.game.events.magic

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player casts a spell on a world object. */
data class MagicOnObjectEvent(
    val client: Client,
    val objectId: Int,
    val position: Position,
    val obj: GameObjectData?,
    val spellId: Int,
) : GameEvent

