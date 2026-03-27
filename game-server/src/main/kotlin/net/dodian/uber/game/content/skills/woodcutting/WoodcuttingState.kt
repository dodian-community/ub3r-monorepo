package net.dodian.uber.game.content.skills.woodcutting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position

data class WoodcuttingState(
    val treeObjectId: Int,
    val treePosition: Position,
    val objectData: GameObjectData?,
    val startedCycle: Long,
    val nextSwingAnimationCycle: Long,
    val nextResourceCycle: Long,
    val resourcesGathered: Int,
)
