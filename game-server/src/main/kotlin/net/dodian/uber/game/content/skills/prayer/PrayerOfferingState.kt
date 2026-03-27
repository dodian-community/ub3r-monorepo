package net.dodian.uber.game.content.skills.prayer

import net.dodian.uber.game.content.skills.core.runtime.SkillActionState

data class PrayerOfferingState(
    val boneItemId: Int,
    val altarX: Int,
    val altarY: Int,
) : SkillActionState
