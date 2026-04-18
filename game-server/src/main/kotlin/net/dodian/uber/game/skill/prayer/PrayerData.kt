package net.dodian.uber.game.skill.prayer

import net.dodian.uber.game.skill.runtime.action.SkillActionRequest
import net.dodian.uber.game.skill.runtime.action.SkillActionState
import net.dodian.uber.game.skill.runtime.action.ActionStopReason

data class PrayerOfferingRequest(
    val boneItemId: Int,
    val altarX: Int,
    val altarY: Int,
) : SkillActionRequest

data class PrayerOfferingState(
    val boneItemId: Int,
    val altarX: Int,
    val altarY: Int,
    override val active: Boolean = true,
    override val startedCycle: Long = 0L,
    override val stopReason: ActionStopReason? = null,
    override val targetRef: String? = null,
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

object PrayerRouteIds {
    const val MAIN_ALTAR_OBJECT_ID: Int = 409
    @JvmField
    val ALTAR_OBJECT_IDS: IntArray = intArrayOf(MAIN_ALTAR_OBJECT_ID, 20377)

    @JvmField
    val BONE_ITEM_IDS: IntArray = Bones.values().map { it.getItemId() }.toIntArray()
}

object PrayerActionIds {
    const val ALTAR_BONES: String = "altar_bones"
}
