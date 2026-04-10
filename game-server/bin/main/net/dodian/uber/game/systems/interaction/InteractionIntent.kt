package net.dodian.uber.game.systems.interaction

interface InteractionIntent {
    val opcode: Int
    val createdCycle: Long
}
