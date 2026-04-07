package net.dodian.uber.game.content.skills.prayer

import net.dodian.uber.game.systems.skills.action.SkillActionRequest
import net.dodian.uber.game.systems.skills.action.SkillActionState

data class PrayerOfferingRequest(
    val boneItemId: Int,
    val altarX: Int,
    val altarY: Int,
) : SkillActionRequest

data class PrayerOfferingState(
    val boneItemId: Int,
    val altarX: Int,
    val altarY: Int,
) : SkillActionState

object PrayerData {
    const val BURY_ANIMATION: Int = 827
    const val ALTAR_ANIMATION: Int = 3705
    const val ALTAR_GFX: Int = 624
    private const val ALTAR_BASE_MULTIPLIER: Double = 2.0

    @JvmStatic
    fun altarMultiplier(firemakingLevel: Int): Double {
        val extra = (firemakingLevel + 1).toDouble() / 100
        return ALTAR_BASE_MULTIPLIER + extra
    }
}
