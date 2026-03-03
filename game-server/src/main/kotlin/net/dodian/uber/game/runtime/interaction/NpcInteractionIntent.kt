package net.dodian.uber.game.runtime.interaction

data class NpcInteractionIntent(
    override val opcode: Int,
    override val createdCycle: Long,
    val npcIndex: Int,
    val option: Int,
) : InteractionIntent
