package net.dodian.uber.game.engine.systems.combat

data class CombatCooldownState(
    val attackStyleAtStart: Int,
    val initialSwingConsumed: Boolean,
    val nextAttackCycle: Long,
    val lastAttackCycle: Long,
)
