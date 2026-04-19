package net.dodian.uber.game.engine.systems.interaction.npcs

import net.dodian.uber.game.engine.systems.follow.FollowRouting
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Utils
import kotlin.math.abs

object BankerApproachFallbackService {
    private val bankerNpcIds = setOf(394, 395, 7677)

    @JvmStatic
    fun shouldAttemptFallback(client: Client, npc: Npc, option: Int): Boolean {
        if (option !in 1..4) {
            return false
        }
        if (npc.id !in bankerNpcIds || client.position.z != npc.position.z) {
            return false
        }
        return true
    }

    @JvmStatic
    fun tryRouteCustomerSide(client: Client, npc: Npc): Boolean {
        val candidates = customerSideCandidates(client, npc)
        if (candidates.isEmpty()) {
            return FollowRouting.routeToEntityBoundary(
                follower = client,
                targetX = npc.position.x,
                targetY = npc.position.y,
                targetSize = npc.size.coerceAtLeast(1),
                z = npc.position.z,
                running = client.buttonOnRun,
            )
        }

        for (candidate in candidates) {
            if (FollowRouting.routeToExactTile(
                    follower = client,
                    destinationX = candidate.first,
                    destinationY = candidate.second,
                    z = npc.position.z,
                    running = client.buttonOnRun,
                )
            ) {
                return true
            }
        }

        return FollowRouting.routeToEntityBoundary(
            follower = client,
            targetX = npc.position.x,
            targetY = npc.position.y,
            targetSize = npc.size.coerceAtLeast(1),
            z = npc.position.z,
            preferredDestination = candidates.firstOrNull(),
            running = client.buttonOnRun,
        )
    }

    private fun customerSideCandidates(client: Client, npc: Npc): List<Pair<Int, Int>> {
        val candidates = LinkedHashSet<Pair<Int, Int>>()
        val preferred = preferredCustomerTile(npc)
        preferred?.let { candidates += it }
        candidates += (npc.position.x - 1) to npc.position.y
        candidates += (npc.position.x + 1) to npc.position.y
        candidates += npc.position.x to (npc.position.y - 1)
        candidates += npc.position.x to (npc.position.y + 1)

        return candidates
            .asSequence()
            .filter { (x, y) -> x != npc.position.x || y != npc.position.y }
            .sortedWith(
                compareBy<Pair<Int, Int>>(
                    { if (preferred != null && it == preferred) 0 else 1 },
                    { abs(it.first - client.position.x) + abs(it.second - client.position.y) },
                ),
            )
            .toList()
    }

    private fun preferredCustomerTile(npc: Npc): Pair<Int, Int>? {
        val face = npc.face
        if (face !in Utils.directionDeltaX.indices) {
            return null
        }
        val dx = Utils.directionDeltaX[face].toInt()
        val dy = Utils.directionDeltaY[face].toInt()
        return npc.position.x - dx to npc.position.y - dy
    }
}
