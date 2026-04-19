package net.dodian.uber.game.engine.systems.interaction

data class GroundItemInteractionIntent(
    override val opcode: Int,
    override val createdCycle: Long,
    val itemId: Int,
    val x: Int,
    val y: Int,
) : InteractionIntent
