package net.dodian.uber.game.content.skills.fishing

import net.dodian.uber.game.content.skills.core.runtime.SkillActionState

data class FishingState(
    val spotIndex: Int,
    val gatheredCount: Int = 0,
) : SkillActionState
