package net.dodian.uber.game.skills.fishing

import net.dodian.uber.game.skills.core.SkillActionState

data class FishingState(
    val spotIndex: Int,
    val gatheredCount: Int = 0,
) : SkillActionState
