package net.dodian.uber.game.systems.interaction

data class MagicOnPlayerIntent(
    override val opcode: Int,
    override val createdCycle: Long,
    val spellId: Int,
    val victimIndex: Int,
) : InteractionIntent
