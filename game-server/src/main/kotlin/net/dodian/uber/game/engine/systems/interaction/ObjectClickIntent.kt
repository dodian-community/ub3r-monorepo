package net.dodian.uber.game.engine.systems.interaction

import net.dodian.cache.objects.GameObjectData
import net.dodian.cache.objects.GameObjectDef
import net.dodian.uber.game.model.Position

data class ObjectClickIntent(
    override val opcode: Int,
    override val createdCycle: Long,
    val option: Int,
    val objectId: Int,
    val objectPosition: Position,
    val objectData: GameObjectData?,
    val objectDef: GameObjectDef?,
) : InteractionIntent
