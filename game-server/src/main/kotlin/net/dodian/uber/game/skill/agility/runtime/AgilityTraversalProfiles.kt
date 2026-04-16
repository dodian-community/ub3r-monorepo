package net.dodian.uber.game.skill.agility.runtime

data class AgilityTraversalProfile(
    val deltaX: Int,
    val deltaY: Int,
    val durationMs: Int,
    val movementAnimationId: Int? = null,
    val startWalkAnimationId: Int? = null,
    val completionAnimationId: Int? = null,
)

object AgilityTraversalProfiles {
    private val byObjectId: Map<Int, AgilityTraversalProfile> =
        mapOf(
            23138 to
                AgilityTraversalProfile(
                    deltaX = 0,
                    deltaY = 7,
                    durationMs = 4200,
                    movementAnimationId = 747,
                    startWalkAnimationId = 746,
                    completionAnimationId = 748,
                ),
            23139 to
                AgilityTraversalProfile(
                    deltaX = 0,
                    deltaY = 7,
                    durationMs = 4200,
                    movementAnimationId = 747,
                    startWalkAnimationId = 746,
                    completionAnimationId = 748,
                ),
        )

    @JvmStatic
    fun profileForObjectId(objectId: Int): AgilityTraversalProfile? = byObjectId[objectId]
}
