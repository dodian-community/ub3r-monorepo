package net.dodian.uber.game.systems.net

sealed interface PacketRequest

data class WalkRequest(
    val opcode: Int,
    val firstStepXAbs: Int,
    val firstStepYAbs: Int,
    val running: Boolean,
    val deltasX: IntArray,
    val deltasY: IntArray,
) : PacketRequest
