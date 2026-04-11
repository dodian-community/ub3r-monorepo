package net.dodian.uber.game.systems.interaction

import net.dodian.uber.game.engine.event.GameEventScheduler
import java.util.concurrent.ConcurrentHashMap
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Player

object PersonalPassageService {
    private data class PassageGrant(
        val allowedEdges: Set<String>,
        val expiresAtMs: Long,
    )

    private val grants = ConcurrentHashMap<String, PassageGrant>()

    @JvmStatic
    fun grantBidirectionalEdges(
        player: Player,
        edges: Iterable<Pair<Position, Position>>,
        durationMs: Long,
    ) {
        val boundedDurationMs = durationMs.coerceAtLeast(1L)
        val expiresAtMs = System.currentTimeMillis() + durationMs.coerceAtLeast(1L)
        val allowedEdges = edges.flatMap { listOf(edgeKey(it.first, it.second), edgeKey(it.second, it.first)) }.toSet()
        val playerKey = playerKey(player)
        val grant = PassageGrant(allowedEdges, expiresAtMs)
        grants[playerKey] = grant

        GameEventScheduler.runLaterMs(boundedDurationMs.toInt()) {
            val current = grants[playerKey] ?: return@runLaterMs
            if (current === grant || current.expiresAtMs <= System.currentTimeMillis()) {
                grants.remove(playerKey, current)
            }
        }
    }

    @JvmStatic
    fun canTraverse(
        player: Player,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        z: Int,
    ): Boolean {
        val key = playerKey(player)
        val grant = grants[key] ?: return false
        if (grant.expiresAtMs < System.currentTimeMillis()) {
            grants.remove(key, grant)
            return false
        }
        return grant.allowedEdges.contains(edgeKey(startX, startY, endX, endY, z))
    }

    @JvmStatic
    fun clearForTests() {
        grants.clear()
    }

    @JvmStatic
    fun clearForPlayer(player: Player) {
        grants.remove(playerKey(player))
    }

    private fun playerKey(player: Player): String {
        val longName = player.longName
        return if (longName > 0L) {
            "long:$longName"
        } else {
            "slot:${player.slot}"
        }
    }

    private fun edgeKey(from: Position, to: Position): String = edgeKey(from.x, from.y, to.x, to.y, from.z)

    private fun edgeKey(fromX: Int, fromY: Int, toX: Int, toY: Int, z: Int): String = "$fromX:$fromY:$toX:$toY:$z"
}
