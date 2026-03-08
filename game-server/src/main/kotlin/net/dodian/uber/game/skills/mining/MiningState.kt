package net.dodian.uber.game.skills.mining

import net.dodian.uber.game.model.Position

data class MiningState(
    val rockObjectId: Int,
    val rockPosition: Position,
    val startedCycle: Long,
    val nextSwingAnimationCycle: Long,
    val nextResourceCycle: Long,
    val resourcesGathered: Int,
)
