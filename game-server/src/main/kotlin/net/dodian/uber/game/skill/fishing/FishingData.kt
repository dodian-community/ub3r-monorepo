package net.dodian.uber.game.skill.fishing

import net.dodian.uber.game.skill.runtime.action.SkillActionRequest
import net.dodian.uber.game.skill.runtime.action.SkillActionState
import net.dodian.uber.game.skill.runtime.action.ActionStopReason

data class FishingSpotDefinition(
    val index: Int,
    val objectId: Int,
    val clickType: Int,
    val fishItemId: Int,
    val animationId: Int,
    val requiredLevel: Int,
    val baseDelayMs: Int,
    val toolItemId: Int,
    val experience: Int,
    val premiumOnly: Boolean = false,
    val featherConsumed: Boolean = false,
)

data class FishingRequest(
    val spotIndex: Int,
) : SkillActionRequest

data class FishingState(
    val spotIndex: Int,
    val gatheredCount: Int = 0,
    override val active: Boolean = true,
    override val startedCycle: Long = 0L,
    override val stopReason: ActionStopReason? = null,
    override val targetRef: String? = null,
) : SkillActionState

object FishingData {
    @JvmField
    val fishingSpots: List<FishingSpotDefinition> = listOf(
        FishingSpotDefinition(0, 1510, 1, 317, 621, 1, 1350, 303, 110),
        FishingSpotDefinition(1, 1510, 2, 335, 622, 20, 1660, 309, 200, featherConsumed = true),
        FishingSpotDefinition(2, 1511, 1, 377, 619, 40, 2480, 301, 440),
        FishingSpotDefinition(3, 1511, 2, 371, 618, 50, 3300, 311, 650),
        FishingSpotDefinition(4, 1514, 1, 7944, 621, 60, 2480, 303, 780, premiumOnly = true),
        FishingSpotDefinition(5, 1514, 2, 383, 618, 70, 4900, 311, 1100, premiumOnly = true),
        FishingSpotDefinition(6, 1517, 1, 395, 619, 85, 5800, 301, 1450, premiumOnly = true),
        FishingSpotDefinition(7, 1517, 2, 389, 618, 95, 6650, 311, 1900, premiumOnly = true),
    )

    @JvmStatic
    fun byIndex(index: Int): FishingSpotDefinition? = fishingSpots.getOrNull(index)

    @JvmStatic
    fun findSpot(objectId: Int, clickType: Int): FishingSpotDefinition? =
        fishingSpots.firstOrNull { it.objectId == objectId && it.clickType == clickType }
}
