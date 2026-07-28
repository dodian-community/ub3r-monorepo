package net.dodian.uber.game.engine.systems.interaction

import net.dodian.cache.objects.GameObjectDef
import net.dodian.uber.game.engine.routing.WorldRouteService
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.objects.WorldObject

internal object ObjectInteractionMovementSettleService {
    fun clearQueuedWalkIfReached(
        player: Client,
        targetPosition: Position,
        objectId: Int,
        objectDef: GameObjectDef?,
    ) {
        val objectType = objectDef?.type ?: 10
        val objectFace = objectDef?.face ?: 0
        val worldObject = WorldObject(objectId, targetPosition.x, targetPosition.y, targetPosition.z, objectType, objectFace)
        if (WorldRouteService.reachedObject(player.position, player.size, worldObject) &&
            player.hasMovementRoute()
        ) {
            player.resetWalkingQueue()
        }
    }
}
