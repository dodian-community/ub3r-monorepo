package net.dodian.uber.game.skills.core.progression

import net.dodian.uber.game.model.player.skills.Skill

data class SkillProgressionResult(
    val success: Boolean,
    val mode: SkillProgressionMode,
    val skill: Skill,
    val oldExperience: Int,
    val newExperience: Int,
    val oldLevel: Int,
    val newLevel: Int,
    val appliedAmount: Int,
)
