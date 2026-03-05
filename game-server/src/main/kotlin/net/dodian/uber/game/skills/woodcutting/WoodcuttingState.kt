package net.dodian.uber.game.skills.woodcutting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position

data class WoodcuttingState(
    val treeObjectId: Int,
    val treePosition: Position,
    val objectData: GameObjectData?,
    val startedAtMs: Long,
    val lastSwingAnimationAtMs: Long,
    val resourcesGathered: Int,
)
