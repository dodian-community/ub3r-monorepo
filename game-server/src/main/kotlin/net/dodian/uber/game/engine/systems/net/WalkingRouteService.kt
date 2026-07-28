package net.dodian.uber.game.engine.systems.net

import kotlin.math.hypot
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.MovementPoint
import net.dodian.uber.game.engine.routing.RouteRequestLoc
import net.dodian.uber.game.engine.routing.WorldRouteService
import net.dodian.uber.game.engine.routing.findRoute
import kotlin.math.abs
import kotlin.math.sign

object WalkingRouteService {
    data class RouteDecision(val waypoints: List<MovementPoint>, val destination: Position)

    /**
     * Validates the bounded client route without searching for a replacement path. Actual
     * collision authority remains in PlayerMovementState as each tile is consumed.
     */
    fun validateClientRoute(player: Client, request: WalkRequest): Boolean {
        val destination = destination(request, player.position.z)
        if (hypot((player.position.x - destination.x).toDouble(), (player.position.y - destination.y).toDouble()) > MAX_CLICK_DISTANCE) {
            player.resetWalkingQueue()
            return false
        }
        val baseX = player.mapRegionX * 8
        val baseY = player.mapRegionY * 8
        val maxX = baseX + REGION_TILE_SIZE - 1
        val maxY = baseY + REGION_TILE_SIZE - 1
        for (index in request.deltasX.indices) {
            val x = request.firstStepXAbs + request.deltasX[index]
            val y = request.firstStepYAbs + request.deltasY[index]
            if (x !in baseX..maxX || y !in baseY..maxY) {
                player.resetWalkingQueue()
                return false
            }
        }
        var currentX = player.position.x
        var currentY = player.position.y
        for (index in request.deltasX.indices) {
            val targetX = request.firstStepXAbs + request.deltasX[index]
            val targetY = request.firstStepYAbs + request.deltasY[index]
            val deltaX = targetX - currentX
            val deltaY = targetY - currentY
            if (deltaX != 0 && deltaY != 0 && abs(deltaX) != abs(deltaY)) return false
            val stepX = deltaX.sign
            val stepY = deltaY.sign
            repeat(maxOf(abs(deltaX), abs(deltaY))) {
                if (!WorldRouteService.canTravel(player.position.z, currentX, currentY, stepX, stepY, player.size)) return false
                currentX += stepX
                currentY += stepY
            }
        }
        return true
    }

    fun preparePlainRoute(player: Client, request: WalkRequest): RouteDecision? {
        val requestedDestination = destination(request, player.position.z)
        if (validateClientRoute(player, request)) {
            return RouteDecision(requestWaypoints(request, player.position.z), requestedDestination)
        }
        val route = WorldRouteService.findRoute(
            RouteRequestLoc(
                level = player.position.z,
                srcX = player.position.x,
                srcY = player.position.y,
                destX = requestedDestination.x,
                destY = requestedDestination.y,
                srcSize = player.size,
                moveNear = true,
            ),
        )
        if (route.failed) {
            player.resetWalkingQueue()
            return null
        }
        val waypoints = route.waypoints
            .take(net.dodian.uber.game.model.entity.player.RouteDestination.MAX_WAYPOINTS)
            .map { MovementPoint(it.x, it.z, it.level) }
        val last = waypoints.lastOrNull()
        val actual = Position(last?.x ?: player.position.x, last?.y ?: player.position.y, player.position.z)
        return RouteDecision(waypoints, actual)
    }

    fun requestWaypoints(request: WalkRequest, z: Int): List<MovementPoint> =
        request.deltasX.indices.map { index ->
            MovementPoint(
                request.firstStepXAbs + request.deltasX[index],
                request.firstStepYAbs + request.deltasY[index],
                z,
            )
        }

    fun destination(request: WalkRequest, z: Int): Position {
        val last = request.deltasX.lastIndex
        val x = request.firstStepXAbs + if (last >= 0) request.deltasX[last] else 0
        val y = request.firstStepYAbs + if (last >= 0) request.deltasY[last] else 0
        return Position(x, y, z)
    }

    fun isPlainWalkOpcode(opcode: Int): Boolean = opcode == 164 || opcode == 248

    private const val MAX_CLICK_DISTANCE = 32.0
    private const val REGION_TILE_SIZE = 104
}
