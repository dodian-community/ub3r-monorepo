package net.dodian.uber.game.model.entity.player

import net.dodian.uber.game.engine.systems.combat.CombatIntent
import net.dodian.uber.game.model.entity.Entity

data class AttackStartDedupeState(
    val intent: CombatIntent,
    val targetType: Entity.Type,
    val targetSlot: Int,
    val acceptedCycle: Long,
)
