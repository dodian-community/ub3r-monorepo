package net.dodian.uber.game.content.objects.travel

import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicInteger
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.`object`.DoorRegistry
import net.dodian.uber.game.systems.api.content.ContentTiming
import net.dodian.uber.game.systems.follow.FollowService
import net.dodian.uber.game.systems.interaction.PersonalPassageService
import net.dodian.uber.game.systems.pathing.AStarPathfindingAlgorithm
import net.dodian.uber.game.systems.pathing.Heuristic
import net.dodian.uber.game.systems.pathing.Node
import net.dodian.uber.game.systems.pathing.collision.CollisionManager
import net.dodian.uber.game.systems.world.player.PlayerRegistry

object LegendsGuildGateService {
    private val leftGate = Position(2728, 3349, 0)
    private val rightGate = Position(2729, 3349, 0)
    private val southLeft = Position(2728, 3348, 0)
    private val southRight = Position(2729, 3348, 0)
    private val northLeft = Position(2728, 3350, 0)
    private val northRight = Position(2729, 3350, 0)
    private const val GATE_OBJECT_LEFT = 2391
    private const val GATE_OBJECT_RIGHT = 2392
    private const val GATE_TYPE = 0
    private const val LEFT_OPEN_FACE = 0
    private const val RIGHT_OPEN_FACE = 2
    private const val PASSAGE_DURATION_MS = 5_000L
    private const val VISUAL_OPEN_MS = 2_400L
    private const val LEFT_CLOSED_FALLBACK_FACE = 1
    private const val RIGHT_CLOSED_FALLBACK_FACE = 3
    private val visualToken = AtomicInteger(0)
    private val pathfinding =
        AStarPathfindingAlgorithm(
            collision = { x, y, z, dx, dy -> CollisionManager.global().traversable(x, y, z, dx, dy) },
            heuristic = Heuristic.EUCLIDEAN,
        )

    @JvmStatic
    fun allowPassage(client: Client): Boolean {
        if (!client.premium) {
            return false
        }
        val lane = resolveLane(client)
        openGateForNearbyPlayers()
        grantPassage(client, lane)
        FollowService.cancelFollow(client)
        return routeTo(client, lane.exit)
    }

    private fun resolveLane(client: Client): Lane {
        val fromSouth = client.position.y <= leftGate.y
        val laneX = if (kotlin.math.abs(client.position.x - southLeft.x) <= kotlin.math.abs(client.position.x - southRight.x)) southLeft.x else southRight.x
        val entryY = if (fromSouth) southLeft.y else northLeft.y
        val exitY = if (fromSouth) northLeft.y else southLeft.y
        val gate = Position(laneX, leftGate.y, leftGate.z)
        val entry = Position(laneX, entryY, leftGate.z)
        val exit = Position(laneX, exitY, leftGate.z)
        return Lane(entry = entry, gate = gate, exit = exit)
    }

    private fun grantPassage(client: Client, lane: Lane) {
        PersonalPassageService.grantBidirectionalEdges(
            client,
            edges =
                listOf(
                    lane.entry to lane.gate,
                    lane.gate to lane.exit,
                ),
            durationMs = PASSAGE_DURATION_MS,
        )
    }

    private fun openGateForNearbyPlayers() {
        val faces = resolveFaces()
        broadcastGateFaces(faces.leftOpen, faces.rightOpen)
        val token = visualToken.incrementAndGet()
        ContentTiming.runLaterMs(VISUAL_OPEN_MS.toInt()) {
            if (visualToken.get() != token) {
                return@runLaterMs
            }
            broadcastGateFaces(faces.leftClosed, faces.rightClosed)
        }
    }

    private fun broadcastGateFaces(leftFace: Int, rightFace: Int) {
        for (player in PlayerRegistry.players) {
            val viewer = player as? Client ?: continue
            if (!viewer.isActive || viewer.disconnected || viewer.position.z != leftGate.z) {
                continue
            }
            if (!viewer.isWithinDistance(viewer.position.x, viewer.position.y, leftGate.x, leftGate.y, 60)) {
                continue
            }
            viewer.ReplaceObject(leftGate.x, leftGate.y, GATE_OBJECT_LEFT, leftFace, GATE_TYPE)
            viewer.ReplaceObject(rightGate.x, rightGate.y, GATE_OBJECT_RIGHT, rightFace, GATE_TYPE)
        }
    }

    private fun resolveFaces(): GateFaces {
        val left = resolveDoorFace(leftGate, GATE_OBJECT_LEFT, LEFT_OPEN_FACE, LEFT_CLOSED_FALLBACK_FACE)
        val right = resolveDoorFace(rightGate, GATE_OBJECT_RIGHT, RIGHT_OPEN_FACE, RIGHT_CLOSED_FALLBACK_FACE)
        return GateFaces(leftOpen = left.open, rightOpen = right.open, leftClosed = left.closed, rightClosed = right.closed)
    }

    private fun resolveDoorFace(position: Position, objectId: Int, fallbackOpen: Int, fallbackClosed: Int): FacePair {
        for (index in DoorRegistry.doorId.indices) {
            if (DoorRegistry.doorId[index] != objectId) continue
            if (DoorRegistry.doorX[index] != position.x || DoorRegistry.doorY[index] != position.y || DoorRegistry.doorHeight[index] != position.z) {
                continue
            }
            val open = DoorRegistry.doorFaceOpen[index]
            val closed = DoorRegistry.doorFaceClosed[index]
            return FacePair(open = open, closed = closed)
        }
        return FacePair(open = fallbackOpen, closed = fallbackClosed)
    }

    private fun routeTo(client: Client, destination: Position): Boolean {
        if (client.position.z != destination.z) {
            return false
        }
        val path = pathfinding.find(client.position.x, client.position.y, destination.x, destination.y, destination.z)
        if (path.isEmpty()) {
            return false
        }
        val validated = validatePath(client, path)
        if (validated.isEmpty()) {
            return false
        }
        applyRoute(client, validated)
        return true
    }

    private fun validatePath(client: Client, path: ArrayDeque<Node>): ArrayDeque<Node> {
        val validated = ArrayDeque<Node>(path.size)
        var currentX = client.position.x
        var currentY = client.position.y
        val z = client.position.z
        for (step in path) {
            val dx = (step.x - currentX).coerceIn(-1, 1)
            val dy = (step.y - currentY).coerceIn(-1, 1)
            if (dx == 0 && dy == 0) {
                continue
            }
            val traversable =
                CollisionManager.global().traversable(step.x, step.y, z, dx, dy) ||
                    PersonalPassageService.canTraverse(client, currentX, currentY, step.x, step.y, z)
            if (!traversable) {
                break
            }
            validated.add(step)
            currentX = step.x
            currentY = step.y
        }
        return validated
    }

    private fun applyRoute(client: Client, path: ArrayDeque<Node>) {
        val steps = minOf(path.size, Player.WALKING_QUEUE_SIZE)
        val baseX = client.mapRegionX * 8
        val baseY = client.mapRegionY * 8
        client.newWalkCmdSteps = steps
        client.newWalkCmdIsRunning = false

        var index = 0
        for (step in path) {
            if (index >= steps) {
                break
            }
            client.newWalkCmdX[index] = step.x - baseX
            client.newWalkCmdY[index] = step.y - baseY
            client.tmpNWCX[index] = client.newWalkCmdX[index]
            client.tmpNWCY[index] = client.newWalkCmdY[index]
            index++
        }
    }

    private data class Lane(
        val entry: Position,
        val gate: Position,
        val exit: Position,
    )

    private data class FacePair(
        val open: Int,
        val closed: Int,
    )

    private data class GateFaces(
        val leftOpen: Int,
        val rightOpen: Int,
        val leftClosed: Int,
        val rightClosed: Int,
    )
}
