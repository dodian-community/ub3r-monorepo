package net.dodian.uber.game.runtime.interaction

import net.dodian.cache.`object`.GameObjectData
import net.dodian.cache.`object`.GameObjectDef
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.WalkToTask

data class ItemOnObjectIntent(
    override val opcode: Int,
    override val createdCycle: Long,
    val interfaceId: Int,
    val itemSlot: Int,
    val itemId: Int,
    val objectId: Int,
    val objectPosition: Position,
    val task: WalkToTask,
    val objectData: GameObjectData?,
    val objectDef: GameObjectDef?,
) : InteractionIntent
