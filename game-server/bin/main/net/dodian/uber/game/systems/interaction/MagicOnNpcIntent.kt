package net.dodian.uber.game.systems.interaction

data class MagicOnNpcIntent(
    override val opcode: Int,
    override val createdCycle: Long,
    val spellId: Int,
    val npcIndex: Int,
) : InteractionIntent
