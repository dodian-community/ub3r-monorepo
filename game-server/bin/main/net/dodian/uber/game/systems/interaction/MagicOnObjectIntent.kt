package net.dodian.uber.game.systems.interaction

import net.dodian.cache.`object`.GameObjectData
import net.dodian.cache.`object`.GameObjectDef
import net.dodian.uber.game.model.Position

data class MagicOnObjectIntent(
    override val opcode: Int,
    override val createdCycle: Long,
    val spellId: Int,
    val objectId: Int,
    val objectPosition: Position,
    val objectData: GameObjectData?,
    val objectDef: GameObjectDef?,
) : InteractionIntent
