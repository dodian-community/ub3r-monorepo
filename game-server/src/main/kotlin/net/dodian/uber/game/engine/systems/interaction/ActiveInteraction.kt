package net.dodian.uber.game.engine.systems.interaction

data class ActiveInteraction(
    val intent: InteractionIntent,
    val startedCycle: Long,
)
