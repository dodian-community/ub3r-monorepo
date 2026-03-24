package net.dodian.uber.game.skills.prayer

import net.dodian.uber.game.skills.core.SkillActionState

data class PrayerOfferingState(
    val boneItemId: Int,
    val altarX: Int,
    val altarY: Int,
) : SkillActionState
