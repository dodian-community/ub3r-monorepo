package net.dodian.uber.game.content.skills.fishing

import net.dodian.uber.game.content.skills.core.runtime.SkillActionRequest

data class FishingRequest(
    val spotIndex: Int,
) : SkillActionRequest
