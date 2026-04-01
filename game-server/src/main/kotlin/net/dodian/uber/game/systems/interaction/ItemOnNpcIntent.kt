package net.dodian.uber.game.systems.interaction

data class ItemOnNpcIntent(
    override val opcode: Int,
    override val createdCycle: Long,
    val itemId: Int,
    val itemSlot: Int,
    val npcIndex: Int,
) : InteractionIntent
