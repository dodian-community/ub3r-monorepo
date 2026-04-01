package net.dodian.uber.game.content.skills.fishing

import net.dodian.uber.game.systems.skills.SkillActionRequest

data class FishingRequest(
    val spotIndex: Int,
) : SkillActionRequest
