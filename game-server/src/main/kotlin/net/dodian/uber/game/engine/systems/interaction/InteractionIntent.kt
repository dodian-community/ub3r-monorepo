package net.dodian.uber.game.engine.systems.interaction

interface InteractionIntent {
    val opcode: Int
    val createdCycle: Long
}
