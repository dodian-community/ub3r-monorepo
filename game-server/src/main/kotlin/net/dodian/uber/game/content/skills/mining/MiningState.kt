package net.dodian.uber.game.content.skills.mining

import net.dodian.uber.game.model.Position

data class MiningState(
    val rockObjectId: Int,
    val rockPosition: Position,
    val startedCycle: Long,
    val resourcesGathered: Int,
)
