package net.dodian.uber.game.engine.systems.interaction

import net.dodian.cache.`object`.GameObjectData
import net.dodian.cache.`object`.GameObjectDef
import net.dodian.uber.game.model.Position

data class ItemOnObjectIntent(
    override val opcode: Int,
    override val createdCycle: Long,
    val interfaceId: Int,
    val itemSlot: Int,
    val itemId: Int,
    val objectId: Int,
    val objectPosition: Position,
    val objectData: GameObjectData?,
    val objectDef: GameObjectDef?,
) : InteractionIntent
