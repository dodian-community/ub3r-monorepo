package net.dodian.uber.game.content.objects.impl.mining

import net.dodian.uber.game.model.Position

data class MiningState(
    val rockObjectId: Int,
    val rockPosition: Position,
    val startedAtMs: Long,
    val lastSwingAnimationAtMs: Long,
    val resourcesGathered: Int,
)
