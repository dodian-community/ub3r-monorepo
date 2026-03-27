package net.dodian.uber.game.content.skills.prayer

import net.dodian.uber.game.content.skills.core.runtime.SkillActionRequest

data class PrayerOfferingRequest(
    val boneItemId: Int,
    val altarX: Int,
    val altarY: Int,
) : SkillActionRequest
