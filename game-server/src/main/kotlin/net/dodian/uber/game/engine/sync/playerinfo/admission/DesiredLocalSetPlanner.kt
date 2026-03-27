package net.dodian.uber.game.engine.sync.playerinfo.admission

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.engine.sync.player.pool.PlayerSyncScratchPool
import net.dodian.uber.game.engine.sync.playerinfo.PlayerVisibilityRules

class DesiredLocalSetPlanner {
    private val scratch = PlayerSyncScratchPool()

    fun build(
        viewer: Client,
        currentLocalSlots: IntArray,
        currentLocalCount: Int,
        candidates: Iterable<Player>,
        maxLocals: Int,
    ): DesiredLocalSet {
        scratch.reset()
        var visibleCandidateCount = 0
        for (other in candidates) {
            if (!PlayerVisibilityRules.isVisibleTo(viewer, other)) {
                continue
            }
            val slot = other.slot
            scratch.mark(slot)
            scratch.candidateSlots[visibleCandidateCount++] = slot
        }

        var desiredCount = 0
        for (i in 0 until currentLocalCount) {
            val slot = currentLocalSlots[i]
            if (slot < 0 || desiredCount >= maxLocals) {
                continue
            }
            if (!scratch.isMarked(slot)) {
                continue
            }
            val player = PlayerHandler.players.getOrNull(slot)
            if (!PlayerVisibilityRules.isVisibleTo(viewer, player)) {
                continue
            }
            scratch.desiredSlots[desiredCount++] = slot
            scratch.markCurrent(slot)
        }

        if (desiredCount < maxLocals) {
            var sortedCount = 0
            for (index in 0 until visibleCandidateCount) {
                val slot = scratch.candidateSlots[index]
                if (slot < 0 || scratch.isCurrent(slot)) {
                    continue
                }
                val player = PlayerHandler.players.getOrNull(slot) ?: continue
                insertSorted(viewer, player, sortedCount)
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
            slots = scratch.desiredSlots,
            count = desiredCount,
            signature = signature,
            isSaturated = desiredCount >= maxLocals,
        )
    }

    fun diff(
        currentLocalSlots: IntArray,
        currentLocalCount: Int,
        desired: DesiredLocalSet,
    ): DesiredLocalSetDiff {
        scratch.reset()
        for (index in 0 until desired.count) {
            val slot = desired.slots[index]
            if (slot >= 0) {
                scratch.mark(slot)
            }
        }

        var removalsCount = 0
        var retainsCount = 0
        var reinsertsCount = 0
        var changedCount = 0
        for (i in 0 until currentLocalCount) {
            val slot = currentLocalSlots[i]
            if (slot < 0) {
                continue
            }
            val player = PlayerHandler.players.getOrNull(slot)
            if (player == null || !scratch.isMarked(slot)) {
                scratch.removals[removalsCount++] = slot
                continue
            }
            if (player.didTeleport()) {
                scratch.removals[removalsCount++] = slot
                scratch.reinserts[reinsertsCount++] = slot
                scratch.markCurrent(slot)
                continue
            }
            scratch.retains[retainsCount++] = slot
            scratch.markCurrent(slot)
            if (player.primaryDirection != -1 || player.secondaryDirection != -1 || player.updateFlags.isUpdateRequired) {
                scratch.changedRetained[changedCount++] = slot
            }
        }

        var additionsCount = 0
        for (index in 0 until desired.count) {
            val slot = desired.slots[index]
            if (slot < 0 || scratch.isCurrent(slot)) {
                continue
            }
            scratch.additions[additionsCount++] = slot
        }

        return DesiredLocalSetDiff(
            removals = scratch.removals,
            removalsCount = removalsCount,
            retains = scratch.retains,
            retainsCount = retainsCount,
            reinserts = scratch.reinserts,
            reinsertsCount = reinsertsCount,
            additions = scratch.additions,
            additionsCount = additionsCount,
            changedRetained = scratch.changedRetained,
            changedRetainedCount = changedCount,
            desiredCount = desired.count,
            desiredSaturated = desired.isSaturated,
        )
    }

    private fun insertSorted(viewer: Client, player: Player, count: Int) {
        val slot = player.slot
        val distance = distance(viewer.position, player.position)
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

    // Visibility gating is performed in the build/diff loops.
}
