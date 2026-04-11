package net.dodian.uber.game.engine.systems.interaction

data class AttackPlayerIntent(
    override val opcode: Int,
    override val createdCycle: Long,
    val victimIndex: Int,
) : InteractionIntent
