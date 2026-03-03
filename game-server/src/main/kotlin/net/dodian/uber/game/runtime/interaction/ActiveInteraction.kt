package net.dodian.uber.game.runtime.interaction

data class ActiveInteraction(
    val intent: InteractionIntent,
    val startedCycle: Long,
)
