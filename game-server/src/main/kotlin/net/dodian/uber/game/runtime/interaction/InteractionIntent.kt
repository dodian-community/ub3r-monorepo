package net.dodian.uber.game.runtime.interaction

interface InteractionIntent {
    val opcode: Int
    val createdCycle: Long
}
