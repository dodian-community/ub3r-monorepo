package net.dodian.uber.game.systems.interaction

data class PlayerInteractionIntent(
    override val opcode: Int,
    override val createdCycle: Long,
    val playerIndex: Int,
    val option: Int,
) : InteractionIntent
