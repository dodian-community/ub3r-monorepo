package net.dodian.uber.game.events

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.engine.event.GameEvent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

data class ItemClickEvent(
    val client: Client,
    val itemId: Int,
    val itemSlot: Int,
    val interfaceId: Int,
) : GameEvent

data class ItemOnItemEvent(
    val client: Client,
    val itemUsedSlot: Int,
    val itemUsedWithSlot: Int,
    val itemUsedId: Int,
    val itemUsedWithId: Int,
) : GameEvent

data class ItemOnObjectEvent(
    val client: Client,
    val objectId: Int,
    val position: Position,
    val obj: GameObjectData?,
    val itemId: Int,
    val itemSlot: Int,
    val interfaceId: Int,
) : GameEvent

data class ItemOnNpcEvent(
    val client: Client,
    val itemId: Int,
    val itemSlot: Int,
    val npcIndex: Int,
    val npc: Npc,
) : GameEvent

data class MagicOnNpcEvent(
    val client: Client,
    val spellId: Int,
    val npcIndex: Int,
    val npc: Npc,
) : GameEvent

data class MagicOnPlayerEvent(
    val client: Client,
    val spellId: Int,
    val victimIndex: Int,
    val victim: Client,
) : GameEvent

data class MagicOnObjectEvent(
    val client: Client,
    val objectId: Int,
    val position: Position,
    val obj: GameObjectData?,
    val spellId: Int,
) : GameEvent
