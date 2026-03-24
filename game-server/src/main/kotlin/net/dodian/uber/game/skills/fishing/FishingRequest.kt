package net.dodian.uber.game.skills.fishing

import net.dodian.uber.game.skills.core.SkillActionRequest

data class FishingRequest(
    val spotIndex: Int,
) : SkillActionRequest
