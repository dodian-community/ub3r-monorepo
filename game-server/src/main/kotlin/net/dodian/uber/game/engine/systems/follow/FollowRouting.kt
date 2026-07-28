package net.dodian.uber.game.engine.systems.follow

import java.util.function.Predicate
import java.util.concurrent.ThreadLocalRandom
import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.routing.RouteRequestLoc
import net.dodian.uber.game.engine.routing.RouteRequestPathingEntity
import net.dodian.uber.game.engine.routing.WorldRouteService
import net.dodian.uber.game.engine.routing.findRoute
import net.dodian.uber.game.model.EntityType
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.MovementPoint
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.objects.WorldObject
import org.rsmod.routefinder.Route
import org.slf4j.LoggerFactory

/** Routes every follow and interaction request through the vendored RS Mod routefinder. */
object FollowRouting {
    private val logger = LoggerFactory.getLogger(FollowRouting::class.java)

    @JvmStatic
    fun enqueueRandomCardinalStep(follower: Client, z: Int): Boolean {
        val first = ThreadLocalRandom.current().nextInt(CARDINAL_OFFSETS.size)
        for (index in CARDINAL_OFFSETS.indices) {
            val offset = CARDINAL_OFFSETS[(first + index) % CARDINAL_OFFSETS.size]
            if (WorldRouteService.canTravel(z, follower.position.x, follower.position.y, offset.first, offset.second, follower.size)) {
                return applyDestination(follower, follower.position.x + offset.first, follower.position.y + offset.second, false)
            }
        }
        return false
    }

    @JvmStatic
    fun isTileOccupied(x: Int, y: Int, z: Int, ignoreNpc: Npc? = null): Boolean {
        val chunks = Server.chunkManager ?: return true
        val center = Position(x, y, z)
        var occupied = false
        chunks.forEachNearby(center, EntityType.PLAYER, 0, Predicate<Player> { it.isActive && !it.disconnected }) { occupied = true }
        if (occupied) return true
        chunks.forEachNearby(center, EntityType.NPC, MAX_NPC_SIZE, Predicate<Npc> { it.isAlive && it !== ignoreNpc }) { npc ->
            val size = npc.size.coerceAtLeast(1)
            if (npc.position.z == z && x in npc.position.x until npc.position.x + size && y in npc.position.y until npc.position.y + size) occupied = true
        }
        return occupied
    }

    @JvmStatic
    @JvmOverloads
    fun routeToEntityBoundary(
        follower: Client,
        targetX: Int,
        targetY: Int,
        targetSize: Int,
        z: Int,
        preferredDestination: Pair<Int, Int>? = null,
        running: Boolean = follower.buttonOnRun,
        targetNpc: Npc? = null,
    ): Boolean {
        if (follower.position.z != z) return false
        if (preferredDestination != null && !isTileOccupied(preferredDestination.first, preferredDestination.second, z, targetNpc)) {
            val preferred = WorldRouteService.findRoute(
                RouteRequestLoc(
                    level = z,
                    srcX = follower.position.x,
                    srcY = follower.position.y,
                    destX = preferredDestination.first,
                    destY = preferredDestination.second,
                    srcSize = follower.size,
                    moveNear = false,
                ),
            )
            if (preferred.success && applyRoute(follower, preferred, running)) return true
        }
        val route = WorldRouteService.findRoute(
            RouteRequestPathingEntity(
                level = z,
                srcX = follower.position.x,
                srcY = follower.position.y,
                destX = targetX,
                destY = targetY,
                srcSize = follower.size,
                destSize = targetSize.coerceAtLeast(1),
            ),
        )
        FollowPathfindingTelemetry.recordSearch(0, !route.failed)
        return !route.failed && applyRoute(follower, route, running)
    }

    @JvmStatic
    fun routeToObjectApproach(
        follower: Client,
        objectId: Int,
        objX: Int,
        objY: Int,
        z: Int,
        type: Int,
        rotation: Int,
        running: Boolean = follower.buttonOnRun,
    ): ObjectRouteResult {
        val worldObject = WorldObject(objectId, objX, objY, z, type, rotation)
        if (WorldRouteService.reachedObject(follower.position, follower.size, worldObject)) {
            return ObjectRouteResult(ObjectRouteStatus.ALREADY_REACHED, follower.position.x, follower.position.y)
        }
        val definition = GameObjectData.forId(objectId)
        val route = WorldRouteService.findRoute(
            RouteRequestLoc(
                level = z,
                srcX = follower.position.x,
                srcY = follower.position.y,
                destX = objX,
                destY = objY,
                srcSize = follower.size,
                destWidth = definition.sizeX,
                destLength = definition.sizeY,
                locAngle = rotation,
                locShape = type,
                moveNear = true,
                blockAccessFlags = definition.walkingFlag and 0xF,
            ),
        )
        if (route.failed) return ObjectRouteResult(ObjectRouteStatus.UNREACHABLE)
        val applied = applyRoute(follower, route, running)
        if (!applied && !WorldRouteService.reachedObject(follower.position, follower.size, worldObject)) {
            return ObjectRouteResult(ObjectRouteStatus.UNREACHABLE)
        }
        val last = route.waypoints.lastOrNull()
        val status = if (route.alternative) ObjectRouteStatus.PARKED_CLOSEST else ObjectRouteStatus.STRICT_REACHED
        return ObjectRouteResult(status, last?.x ?: follower.position.x, last?.z ?: follower.position.y)
    }

    @JvmStatic
    fun routeToObjectInteraction(follower: Client, objectId: Int, objX: Int, objY: Int, z: Int, type: Int, rotation: Int, running: Boolean = follower.buttonOnRun): Boolean =
        routeToObjectApproach(follower, objectId, objX, objY, z, type, rotation, running).status in setOf(ObjectRouteStatus.ALREADY_REACHED, ObjectRouteStatus.STRICT_REACHED)

    @JvmStatic
    fun routeToObjectVicinity(follower: Client, objectId: Int, objX: Int, objY: Int, z: Int, rotation: Int, running: Boolean = follower.buttonOnRun): Boolean =
        routeToObjectApproach(follower, objectId, objX, objY, z, 10, rotation, running).status != ObjectRouteStatus.UNREACHABLE

    @JvmStatic
    fun routeToExactTile(follower: Client, destinationX: Int, destinationY: Int, z: Int, running: Boolean = follower.buttonOnRun): Boolean {
        if (WorldRouteService.isTileBlocked(destinationX, destinationY, z)) return false
        val route = WorldRouteService.findRoute(
            RouteRequestLoc(
                level = z,
                srcX = follower.position.x,
                srcY = follower.position.y,
                destX = destinationX,
                destY = destinationY,
                srcSize = follower.size,
                moveNear = false,
            ),
        )
        return route.success && applyRoute(follower, route, running)
    }

    private fun applyRoute(follower: Client, route: Route, running: Boolean): Boolean {
        if (route.waypoints.isEmpty()) return route.success
        val waypoints = route.waypoints
            .take(net.dodian.uber.game.model.entity.player.RouteDestination.MAX_WAYPOINTS)
            .map { MovementPoint(it.x, it.z, it.level) }
        follower.replaceMovementRoute(waypoints, running)
        return true
    }

    private fun applyDestination(follower: Client, x: Int, y: Int, running: Boolean): Boolean {
        follower.replaceMovementRoute(listOf(MovementPoint(x, y, follower.position.z)), running)
        return true
    }

    private val CARDINAL_OFFSETS = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)
    private const val MAX_NPC_SIZE = 8
}

data class ObjectRouteResult(val status: ObjectRouteStatus, val targetX: Int? = null, val targetY: Int? = null)

enum class ObjectRouteStatus { ALREADY_REACHED, STRICT_REACHED, PARKED_CLOSEST, UNREACHABLE }
