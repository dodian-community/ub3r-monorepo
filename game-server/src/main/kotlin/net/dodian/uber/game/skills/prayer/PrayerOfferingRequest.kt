package net.dodian.uber.game.skills.prayer

import net.dodian.uber.game.skills.core.SkillActionRequest

data class PrayerOfferingRequest(
    val boneItemId: Int,
    val altarX: Int,
    val altarY: Int,
) : SkillActionRequest
