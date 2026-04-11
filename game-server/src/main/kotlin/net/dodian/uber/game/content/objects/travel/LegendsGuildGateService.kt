package net.dodian.uber.game.content.objects.travel

import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
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
import org.slf4j.LoggerFactory

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
    private const val LEGENDS_GUARD_NPC_ID = 3951
    private val DEFAULT_LEFT_GATE_FACES = FacePair(open = 0, closed = -3)
    private val DEFAULT_RIGHT_GATE_FACES = FacePair(open = -2, closed = -3)
    private const val PASSAGE_DURATION_MS = 5_000L
    private const val VISUAL_OPEN_MS = 2_400L
    private const val TRAVERSAL_TICK_MS = 600
    private const val TRAVERSAL_TIMEOUT_MS = 8_000L

    private enum class Stage {
        TO_ENTRY,
        TO_EXIT,
    }

    private data class Lane(
        val id: String,
        val entry: Position,
        val gate: Position,
        val exit: Position,
    )

    internal data class FacePair(
        val open: Int,
        val closed: Int,
    )

    internal data class VisualSnapshot(
        val left: FacePair?,
        val right: FacePair?,
    )

    private data class ActiveTraversal(
        val playerKey: String,
        val lane: Lane,
        val deadlineMs: Long,
        var stage: Stage = Stage.TO_ENTRY,
    )

    private val logger = LoggerFactory.getLogger(LegendsGuildGateService::class.java)
    private val visualToken = AtomicInteger(0)
    private val activeTraversals = ConcurrentHashMap<String, ActiveTraversal>()
    private val completionReasonsForTests = ConcurrentHashMap<String, String>()

    @Volatile
    private var lastVisualSnapshotForTests: VisualSnapshot? = null

    @JvmStatic
    fun allowPassage(client: Client): Boolean {
        if (!client.premium) {
            return false
        }

        val lane = resolveLane(client)
        val key = playerKey(client)
        if (activeTraversals.containsKey(key)) {
            return true
        }

        FollowService.cancelFollow(client)
        val alreadyAtEntry = isAtPosition(client, lane.entry)
        if (!alreadyAtEntry && !routeTo(client, lane.entry)) {
            finishTraversal(client, lane, "entry_path_missing", clearPassage = true)
            return false
        }
        logger.info(
            "Legends gate init player={} lane={} route={}",
            client.playerName,
            lane.id,
            if (alreadyAtEntry) "already_at_entry" else "entry_path_found",
        )
        openGateForNearbyPlayers()

        val traversal =
            ActiveTraversal(
                playerKey = key,
                lane = lane,
                deadlineMs = System.currentTimeMillis() + TRAVERSAL_TIMEOUT_MS,
            )
        activeTraversals[key] = traversal
        if (!tickTraversal(client, traversal)) {
            return completionReasonsForTests[key] == "success"
        }
        scheduleTraversalTick(client, traversal)
        return true
    }

    @JvmStatic
    fun primeGuardApproach(client: Client, npc: Npc): Boolean {
        if (!client.premium || npc.id != LEGENDS_GUARD_NPC_ID || !isLegendsGuard(npc.position)) {
            return false
        }

        PersonalPassageService.grantBidirectionalEdges(
            client,
            edges = guardApproachEdges(fromSouth = client.position.y <= leftGate.y),
            durationMs = PASSAGE_DURATION_MS,
        )
        openGateForNearbyPlayers()
        return true
    }

    private fun scheduleTraversalTick(client: Client, traversal: ActiveTraversal) {
        ContentTiming.runRepeatingMs(
            delayMs = TRAVERSAL_TICK_MS,
            intervalMs = TRAVERSAL_TICK_MS,
        ) {
            tickTraversal(client, traversal)
        }
    }

    private fun tickTraversal(client: Client, traversal: ActiveTraversal): Boolean {
        val current = activeTraversals[traversal.playerKey] ?: return false
        if (current !== traversal) {
            return false
        }

        if (client.disconnected || !client.isActive) {
            cleanup(client)
            return false
        }

        if (System.currentTimeMillis() > traversal.deadlineMs) {
            finishTraversal(client, traversal.lane, "timeout", clearPassage = true)
            return false
        }

        return when (traversal.stage) {
            Stage.TO_ENTRY -> handleEntryStage(client, traversal)
            Stage.TO_EXIT -> handleExitStage(client, traversal)
        }
    }

    private fun handleEntryStage(client: Client, traversal: ActiveTraversal): Boolean {
        if (isAtAndSettled(client, traversal.lane.entry)) {
            grantPassage(client, traversal.lane)
            if (!routeAcrossGate(client, traversal.lane)) {
                finishTraversal(client, traversal.lane, "cross_path_missing", clearPassage = true)
                return false
            }
            traversal.stage = Stage.TO_EXIT
            return true
        }

        if (isMovementSettled(client) && !hasPendingWalkCommand(client) && !routeTo(client, traversal.lane.entry)) {
            finishTraversal(client, traversal.lane, "entry_path_missing", clearPassage = true)
            return false
        }

        return true
    }

    private fun handleExitStage(client: Client, traversal: ActiveTraversal): Boolean {
        if (isAtAndSettled(client, traversal.lane.exit)) {
            finishTraversal(client, traversal.lane, "success", clearPassage = true)
            return false
        }

        if (isMovementSettled(client) && !hasPendingWalkCommand(client) && !routeAcrossGate(client, traversal.lane)) {
            finishTraversal(client, traversal.lane, "cross_path_missing", clearPassage = true)
            return false
        }

        return true
    }

    private fun resolveLane(client: Client): Lane {
        val fromSouth = client.position.y <= leftGate.y
        val laneX =
            if (kotlin.math.abs(client.position.x - southLeft.x) <= kotlin.math.abs(client.position.x - southRight.x)) {
                southLeft.x
            } else {
                southRight.x
            }

        val entryY = if (fromSouth) southLeft.y else northLeft.y
        val exitY = if (fromSouth) northLeft.y else southLeft.y
        val direction = if (fromSouth) "south_to_north" else "north_to_south"
        val side = if (laneX == leftGate.x) "left" else "right"

        return Lane(
            id = "$side:$direction",
            entry = Position(laneX, entryY, leftGate.z),
            gate = Position(laneX, leftGate.y, leftGate.z),
            exit = Position(laneX, exitY, leftGate.z),
        )
    }

    private fun grantPassage(client: Client, lane: Lane) {
        PersonalPassageService.grantBidirectionalEdges(
            client,
            edges =
                listOf(
                    lane.entry to lane.gate,
                    lane.gate to lane.exit,
                ) + guardApproachEdges(fromSouth = lane.entry.y < leftGate.y),
            durationMs = PASSAGE_DURATION_MS,
        )
    }

    private fun guardApproachEdges(fromSouth: Boolean): List<Pair<Position, Position>> {
        val frontTiles = if (fromSouth) listOf(southLeft, southRight) else listOf(northLeft, northRight)
        return buildList {
            for (frontTile in frontTiles) {
                add(frontTile to leftGate)
                add(frontTile to rightGate)
            }
        }
    }

    private fun isLegendsGuard(position: Position): Boolean =
        (position.x == 2727 || position.x == 2730) && position.y == 3349 && position.z == 0

    private fun openGateForNearbyPlayers() {
        val snapshot = resolveVisualSnapshot()
        lastVisualSnapshotForTests = snapshot
        broadcastGateFaces(snapshot, open = true)
        val token = visualToken.incrementAndGet()
        ContentTiming.runLaterMs(VISUAL_OPEN_MS.toInt()) {
            if (visualToken.get() != token) {
                return@runLaterMs
            }
            broadcastGateFaces(snapshot, open = false)
        }
    }

    private fun resolveVisualSnapshot(): VisualSnapshot {
        val left = resolveDoorFace(leftGate, GATE_OBJECT_LEFT)
        val right = resolveDoorFace(rightGate, GATE_OBJECT_RIGHT)
        return VisualSnapshot(left = left, right = right)
    }

    private fun resolveDoorFace(position: Position, objectId: Int): FacePair? {
        for (index in DoorRegistry.doorId.indices) {
            if (DoorRegistry.doorId[index] != objectId) {
                continue
            }
            if (DoorRegistry.doorX[index] != position.x || DoorRegistry.doorY[index] != position.y || DoorRegistry.doorHeight[index] != position.z) {
                continue
            }
            return FacePair(open = DoorRegistry.doorFaceOpen[index], closed = DoorRegistry.doorFaceClosed[index])
        }
        return fallbackDoorFace(position, objectId)
    }

    private fun fallbackDoorFace(position: Position, objectId: Int): FacePair? {
        return when {
            objectId == GATE_OBJECT_LEFT && position == leftGate -> DEFAULT_LEFT_GATE_FACES
            objectId == GATE_OBJECT_RIGHT && position == rightGate -> DEFAULT_RIGHT_GATE_FACES
            else -> null
        }
    }

    private fun broadcastGateFaces(snapshot: VisualSnapshot, open: Boolean) {
        val leftFace = if (open) snapshot.left?.open else snapshot.left?.closed
        val rightFace = if (open) snapshot.right?.open else snapshot.right?.closed
        if (leftFace == null && rightFace == null) {
            return
        }

        for (player in PlayerRegistry.players) {
            val viewer = player as? Client ?: continue
            if (!viewer.isActive || viewer.disconnected || viewer.position.z != leftGate.z) {
                continue
            }
            if (!viewer.isWithinDistance(viewer.position.x, viewer.position.y, leftGate.x, leftGate.y, 60)) {
                continue
            }
            if (leftFace != null) {
                viewer.ReplaceObject(leftGate.x, leftGate.y, GATE_OBJECT_LEFT, leftFace, GATE_TYPE)
            }
            if (rightFace != null) {
                viewer.ReplaceObject(rightGate.x, rightGate.y, GATE_OBJECT_RIGHT, rightFace, GATE_TYPE)
            }
        }
    }

    private fun routeAcrossGate(client: Client, lane: Lane): Boolean {
        if (client.position.z != lane.exit.z) {
            return false
        }

        val path = ArrayDeque<Node>(2)
        path.add(Node(lane.gate.x, lane.gate.y, lane.gate.z))
        path.add(Node(lane.exit.x, lane.exit.y, lane.exit.z))

        val validated = validatePath(client, path)
        if (validated.isEmpty()) {
            return false
        }

        val finalStep = validated.lastOrNull() ?: return false
        if (finalStep.x != lane.exit.x || finalStep.y != lane.exit.y) {
            return false
        }

        applyRoute(client, validated)
        return true
    }

    private fun routeTo(client: Client, destination: Position): Boolean {
        if (client.position.z != destination.z) {
            return false
        }
        if (isAtPosition(client, destination)) {
            return true
        }

        val pathfinder =
            AStarPathfindingAlgorithm(
                collision = { x, y, z, dx, dy -> CollisionManager.global().traversable(x, y, z, dx, dy) },
                heuristic = Heuristic.EUCLIDEAN,
            )
        val path = pathfinder.find(client.position.x, client.position.y, destination.x, destination.y, destination.z)
        if (path.isEmpty()) {
            return false
        }

        val validated = validatePath(client, path)
        if (validated.isEmpty()) {
            return false
        }

        val finalStep = validated.lastOrNull() ?: return false
        if (finalStep.x != destination.x || finalStep.y != destination.y) {
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
        client.resetWalkingQueue()
        client.newWalkCmdSteps = 0
        client.newWalkCmdIsRunning = false
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

    private fun finishTraversal(client: Client, lane: Lane, reason: String, clearPassage: Boolean) {
        cleanup(client, clearPassage)
        completionReasonsForTests[playerKey(client)] = reason
        logger.info(
            "Legends gate traversal player={} lane={} result={}",
            client.playerName,
            lane.id,
            reason,
        )
    }

    private fun cleanup(client: Client, clearPassage: Boolean = true) {
        val key = playerKey(client)
        activeTraversals.remove(key)
        if (clearPassage) {
            PersonalPassageService.clearForPlayer(client)
        }
    }

    private fun isAtAndSettled(client: Client, position: Position): Boolean {
        return client.position.x == position.x && client.position.y == position.y && client.position.z == position.z && isMovementSettled(client)
    }

    private fun isAtPosition(client: Client, position: Position): Boolean =
        client.position.x == position.x && client.position.y == position.y && client.position.z == position.z

    private fun isMovementSettled(client: Client): Boolean {
        return client.primaryDirection == -1 && client.secondaryDirection == -1 && client.wQueueReadPtr == client.wQueueWritePtr
    }

    private fun hasPendingWalkCommand(client: Client): Boolean = client.newWalkCmdSteps > 0

    private fun playerKey(player: Player): String {
        val longName = player.longName
        return if (longName > 0L) {
            "long:$longName"
        } else {
            "slot:${player.slot}"
        }
    }

    internal fun completionReasonForTests(client: Client): String? = completionReasonsForTests[playerKey(client)]

    internal fun visualSnapshotForTests(): VisualSnapshot? = lastVisualSnapshotForTests

    internal fun pumpTraversalForTests(client: Client) {
        val traversal = activeTraversals[playerKey(client)] ?: return
        tickTraversal(client, traversal)
    }

    internal fun clearForTests() {
        activeTraversals.clear()
        completionReasonsForTests.clear()
        lastVisualSnapshotForTests = null
        visualToken.set(0)
    }
}
