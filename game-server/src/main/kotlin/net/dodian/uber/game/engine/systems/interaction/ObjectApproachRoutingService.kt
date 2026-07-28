package net.dodian.uber.game.engine.systems.interaction

import net.dodian.cache.objects.GameObjectData
import net.dodian.cache.objects.GameObjectDef
import net.dodian.uber.game.engine.systems.follow.FollowRouting
import net.dodian.uber.game.engine.systems.follow.ObjectRouteStatus
import net.dodian.uber.game.engine.routing.WorldRouteService
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.objects.WorldObject
import org.slf4j.LoggerFactory

internal object ObjectApproachRoutingService {
    private val logger = LoggerFactory.getLogger(ObjectApproachRoutingService::class.java)
    fun ensureReached(
        player: Client,
        objectId: Int,
        targetPosition: Position,
        objectData: GameObjectData?,
        objectDef: GameObjectDef?,
    ): ApproachResult {
        if (objectData == null && objectDef == null) {
            return ApproachResult(ApproachStatus.MISSING_OBJECT)
        }

        val worldObject = worldObject(objectId, targetPosition, objectDef)
        val reached = WorldRouteService.reachedObject(player.position, player.size, worldObject)
        logger.debug(
            "[ObjApproach] objectId={} pos=({},{},{}) player=({},{}) reached={} routeWaypoints={}",
            objectId, targetPosition.x, targetPosition.y, targetPosition.z,
            player.position.x, player.position.y, reached,
            player.movementRouteSize(),
        )
        if (reached) {
            // Clear any remaining walk steps now that we've arrived — mirrors tarnish's
            // WalkToWaypoint.onDestination() → mob.movement.reset() sequence.
            if (player.hasMovementRoute()) {
                player.resetWalkingQueue()
            }
            return ApproachResult(ApproachStatus.REACHED)
        }

        // Compute route. The caller (InteractionProcessor) is responsible for guarding against
        // re-routing every tick when a walk is already in progress (via lastRoutePosition tracking,
        // matching tarnish's Waypoint.lastPosition check).
        // FollowRouting.routeToObjectApproach handles applying/resetting the walk queue internally.
        val route =
            FollowRouting.routeToObjectApproach(
                follower = player,
                objectId = objectId,
                objX = targetPosition.x,
                objY = targetPosition.y,
                z = targetPosition.z,
                type = worldObject.type,
                rotation = worldObject.face,
            )
        if (route.status == ObjectRouteStatus.UNREACHABLE) {
            return ApproachResult(ApproachStatus.UNREACHABLE)
        }
        // If route applied steps, report ROUTED so the caller waits for movement.
        if (player.hasMovementRoute()) {
            return ApproachResult(ApproachStatus.ROUTED, route.targetX, route.targetY)
        }
        return when (route.status) {
            ObjectRouteStatus.ALREADY_REACHED,
            ObjectRouteStatus.STRICT_REACHED,
            -> ApproachResult(ApproachStatus.REACHED, route.targetX, route.targetY)
            ObjectRouteStatus.PARKED_CLOSEST -> ApproachResult(ApproachStatus.PARKED_CLOSEST, route.targetX, route.targetY)
            ObjectRouteStatus.UNREACHABLE -> ApproachResult(ApproachStatus.UNREACHABLE)
        }
    }

    private fun worldObject(objectId: Int, targetPosition: Position, objectDef: GameObjectDef?): WorldObject =
        WorldObject(
            objectId,
            targetPosition.x,
            targetPosition.y,
            targetPosition.z,
            objectDef?.type ?: DEFAULT_OBJECT_TYPE,
            objectDef?.face ?: DEFAULT_OBJECT_ROTATION,
        )

    private const val DEFAULT_OBJECT_TYPE = 10
    private const val DEFAULT_OBJECT_ROTATION = 0
}

internal data class ApproachResult(
    val status: ApproachStatus,
    val targetX: Int? = null,
    val targetY: Int? = null,
)

internal enum class ApproachStatus {
    REACHED,
    ROUTED,
    PARKED_CLOSEST,
    UNREACHABLE,
    MISSING_OBJECT,
}
