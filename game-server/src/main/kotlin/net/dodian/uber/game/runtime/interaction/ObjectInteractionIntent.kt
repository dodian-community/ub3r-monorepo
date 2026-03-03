package net.dodian.uber.game.runtime.interaction

data class ObjectInteractionIntent(
    override val opcode: Int,
    override val createdCycle: Long,
    val objectId: Int,
    val x: Int,
    val y: Int,
    val option: Int,
) : InteractionIntent
