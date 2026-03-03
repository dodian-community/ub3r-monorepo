package net.dodian.uber.game.runtime.sync.player.root

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.runtime.sync.player.pool.PlayerSyncScratchPool

class DesiredLocalSetPlanner {
    private val scratch = PlayerSyncScratchPool()

    fun build(
        viewer: Client,
        currentLocalSlots: IntArray,
        visibleSlots: IntArray,
        maxLocals: Int,
    ): DesiredLocalSet {
        scratch.reset()
        var visibleCandidateCount = 0
        for (slot in visibleSlots) {
            if (slot < 0) {
                continue
            }
            scratch.mark(slot)
            scratch.candidateSlots[visibleCandidateCount++] = slot
        }

        var desiredCount = 0
        for (slot in currentLocalSlots) {
            if (slot < 0 || desiredCount >= maxLocals) {
                continue
            }
            if (!scratch.isMarked(slot)) {
                continue
            }
            val player = resolveVisiblePlayer(viewer, slot) ?: continue
            scratch.desiredSlots[desiredCount++] = player.slot
            scratch.markCurrent(player.slot)
        }

        if (desiredCount < maxLocals) {
            var sortedCount = 0
            for (index in 0 until visibleCandidateCount) {
                val slot = scratch.candidateSlots[index]
                if (slot < 0 || scratch.isCurrent(slot)) {
                    continue
                }
                val player = resolveVisiblePlayer(viewer, slot) ?: continue
                insertSorted(viewer, player.slot, sortedCount)
                sortedCount++
            }

            for (index in 0 until sortedCount) {
                if (desiredCount >= maxLocals) {
                    break
                }
                scratch.desiredSlots[desiredCount++] = scratch.sortedCandidateSlots[index]
            }
        }

        var signature = 1
        for (index in 0 until desiredCount) {
            signature = 31 * signature + scratch.desiredSlots[index]
        }
        return DesiredLocalSet(
            slots = scratch.desiredSlots.copyOf(desiredCount),
            count = desiredCount,
            signature = signature,
            isSaturated = desiredCount >= maxLocals,
        )
    }

    fun diff(
        currentLocalSlots: IntArray,
        desired: DesiredLocalSet,
    ): DesiredLocalSetDiff {
        scratch.reset()
        for (slot in desired.slots) {
            if (slot >= 0) {
                scratch.mark(slot)
            }
        }

        var removalsCount = 0
        var retainsCount = 0
        var changedCount = 0
        for (slot in currentLocalSlots) {
            if (slot < 0) {
                continue
            }
            val player = PlayerHandler.players.getOrNull(slot)
            if (player == null || !scratch.isMarked(slot)) {
                scratch.removals[removalsCount++] = slot
                continue
            }
            scratch.retains[retainsCount++] = slot
            scratch.markCurrent(slot)
            if (player.primaryDirection != -1 || player.secondaryDirection != -1 || player.updateFlags.isUpdateRequired) {
                scratch.changedRetained[changedCount++] = slot
            }
        }

        var additionsCount = 0
        for (slot in desired.slots) {
            if (slot < 0 || scratch.isCurrent(slot)) {
                continue
            }
            scratch.additions[additionsCount++] = slot
        }

        return DesiredLocalSetDiff(
            removals = scratch.removals.copyOf(removalsCount),
            retains = scratch.retains.copyOf(retainsCount),
            additions = scratch.additions.copyOf(additionsCount),
            changedRetained = scratch.changedRetained.copyOf(changedCount),
            desiredCount = desired.count,
            desiredSaturated = desired.isSaturated,
        )
    }

    private fun insertSorted(viewer: Client, slot: Int, count: Int) {
        val distance = distance(viewer.position, PlayerHandler.players[slot]?.position)
        var index = count
        while (index > 0) {
            val previousSlot = scratch.sortedCandidateSlots[index - 1]
            val previousDistance = scratch.sortedCandidateDistances[index - 1]
            if (previousDistance < distance || (previousDistance == distance && previousSlot <= slot)) {
                break
            }
            scratch.sortedCandidateSlots[index] = previousSlot
            scratch.sortedCandidateDistances[index] = previousDistance
            index--
        }
        scratch.sortedCandidateSlots[index] = slot
        scratch.sortedCandidateDistances[index] = distance
    }

    private fun distance(a: Position?, b: Position?): Int {
        if (a == null || b == null) {
            return Int.MAX_VALUE
        }
        val dx = kotlin.math.abs(a.x - b.x)
        val dy = kotlin.math.abs(a.y - b.y)
        return maxOf(dx, dy)
    }

    private fun resolveVisiblePlayer(viewer: Client, slot: Int): Player? {
        val player = PlayerHandler.players.getOrNull(slot) ?: return null
        if (player === viewer || !player.isActive) {
            return null
        }
        if (!viewer.withinDistance(player)) {
            return null
        }
        if (player.invis && !viewer.invis) {
            return null
        }
        return player
    }
}
