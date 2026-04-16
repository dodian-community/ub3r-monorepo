package net.dodian.uber.game.engine.systems.interaction.npcs

import net.dodian.uber.game.engine.systems.follow.FollowRouting
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Utils

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
        if (client.newWalkCmdSteps > 0 || client.wQueueReadPtr != client.wQueueWritePtr) {
            return false
        }
        if (client.primaryDirection != -1 || client.secondaryDirection != -1) {
            return false
        }
        return true
    }

    @JvmStatic
    fun tryRouteCustomerSide(client: Client, npc: Npc): Boolean {
        val preferred = preferredCustomerTile(npc)
        return FollowRouting.routeToEntityBoundary(
            follower = client,
            targetX = npc.position.x,
            targetY = npc.position.y,
            targetSize = npc.size.coerceAtLeast(1),
            z = npc.position.z,
            preferredDestination = preferred,
            running = client.buttonOnRun,
        )
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
