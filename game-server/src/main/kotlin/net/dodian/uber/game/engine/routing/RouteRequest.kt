package net.dodian.uber.game.engine.routing

/**
 * A route request whose destination is a static world location - a bare tile
 * (defaults) or a loc/object footprint (`destWidth`/`destLength`/`locAngle`/
 * `locShape`/`blockAccessFlags` set to match the object's definition).
 */
data class RouteRequestLoc(
    val level: Int,
    val srcX: Int,
    val srcY: Int,
    val destX: Int,
    val destY: Int,
    val srcSize: Int = 1,
    val destWidth: Int = 1,
    val destLength: Int = 1,
    val locAngle: Int = 0,
    val locShape: Int = -1,
    val moveNear: Boolean = true,
    val blockAccessFlags: Int = 0,
)

/**
 * A route request whose destination is another entity's current boundary
 * (following, or interacting with a moving [net.dodian.uber.game.model.entity.player.Player]/
 * [net.dodian.uber.game.model.entity.npc.Npc]) - always routes to the
 * entity's edge (`locShape = -2`) and moves as close as possible.
 */
data class RouteRequestPathingEntity(
    val level: Int,
    val srcX: Int,
    val srcY: Int,
    val destX: Int,
    val destY: Int,
    val srcSize: Int = 1,
    val destSize: Int = 1,
)

fun WorldRouteService.findRoute(request: RouteRequestLoc): org.rsmod.routefinder.Route = findRoute(
    level = request.level,
    srcX = request.srcX,
    srcY = request.srcY,
    destX = request.destX,
    destY = request.destY,
    srcSize = request.srcSize,
    destWidth = request.destWidth,
    destLength = request.destLength,
    locAngle = request.locAngle,
    locShape = request.locShape,
    moveNear = request.moveNear,
    blockAccessFlags = request.blockAccessFlags,
)

fun WorldRouteService.findRoute(request: RouteRequestPathingEntity): org.rsmod.routefinder.Route = findRoute(
    level = request.level,
    srcX = request.srcX,
    srcY = request.srcY,
    destX = request.destX,
    destY = request.destY,
    srcSize = request.srcSize,
    destWidth = request.destSize.coerceAtLeast(1),
    destLength = request.destSize.coerceAtLeast(1),
    locShape = -2,
    moveNear = true,
)
