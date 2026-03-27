package net.dodian.uber.game.systems.world.farming

import com.google.gson.JsonArray
import java.util.IdentityHashMap
import java.util.TreeMap
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.farming.FarmingState
import net.dodian.uber.game.content.skills.farming.FarmingDefinitions
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import net.dodian.uber.game.systems.world.pulse.GlobalPulseService

data class FarmingRefreshStats(
    val activePlayers: Int,
)

data class FarmingRunStats(
    val duePlayers: Int,
    val processedPlayers: Int,
    val maxBucketSize: Int,
)

class FarmingScheduler {
    private val scheduled = IdentityHashMap<Client, Long>()
    private val buckets = TreeMap<Long, FarmingTickBucket>()
    private val waterSaplingIds =
        FarmingDefinitions.sapling.values().mapTo(HashSet()) { it.waterId + 1 }

    fun refreshActivePlayers(
        activePlayers: List<Client>,
        currentCycle: Long,
    ): FarmingRefreshStats {
        val activeSet = IdentityHashMap<Client, Boolean>(activePlayers.size)
        activePlayers.forEach { client ->
            activeSet[client] = true
            if (!scheduled.containsKey(client)) {
                val interval = if (hasActiveWork(client)) ACTIVE_INTERVAL_TICKS else IDLE_RECHECK_INTERVAL_TICKS
                schedule(client, alignedCycle(client, currentCycle, interval))
            }
        }

        // Do not call unschedule() while iterating scheduled: that would mutate the same map
        // and trigger CME. Remove from buckets using the known scheduled cycle value instead.
        val iterator = scheduled.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val client = entry.key
            if (activeSet.containsKey(client)) {
                continue
            }
            val existing = entry.value
            val bucket = buckets[existing]
            if (bucket != null) {
                bucket.duePlayers.remove(client)
                if (bucket.duePlayers.isEmpty()) {
                    buckets.remove(existing)
                }
            }
            iterator.remove()
        }
        return FarmingRefreshStats(activePlayers.size)
    }

    fun noteActivity(player: Client, currentCycle: Long = PlayerRegistry.cycle.toLong()) {
        schedule(player, alignedCycle(player, currentCycle, ACTIVE_INTERVAL_TICKS))
    }

    fun runDue(currentCycle: Long): FarmingRunStats {
        var dueCount = 0
        var processed = 0
        var maxBucketSize = 0
        while (buckets.isNotEmpty()) {
            val entry = buckets.firstEntry()
            if (entry.key > currentCycle) {
                break
            }
            buckets.pollFirstEntry()
            val players = entry.value.duePlayers.drain()
            if (players.size > maxBucketSize) {
                maxBucketSize = players.size
            }
            dueCount += players.size
            players.forEach { client ->
                scheduled.remove(client)
                if (!client.isActive || client.disconnected) {
                    return@forEach
                }
                client.farming.run { client.updateFarming() }
                processed++
                val interval = if (hasActiveWork(client)) ACTIVE_INTERVAL_TICKS else IDLE_RECHECK_INTERVAL_TICKS
                schedule(client, alignedCycle(client, currentCycle, interval))
            }
        }
        return FarmingRunStats(dueCount, processed, maxBucketSize)
    }

    private fun schedule(
        player: Client,
        cycle: Long,
    ) {
        unschedule(player)
        scheduled[player] = cycle
        buckets.computeIfAbsent(cycle) { FarmingTickBucket(cycle) }.duePlayers.add(player)
    }

    private fun unschedule(player: Client) {
        val existing = scheduled.remove(player) ?: return
        val bucket = buckets[existing] ?: return
        bucket.duePlayers.remove(player)
        if (bucket.duePlayers.isEmpty()) {
            buckets.remove(existing)
        }
    }

    private fun hasActiveWork(client: Client): Boolean {
        if (hasPendingSaplings(client)) {
            return true
        }
        if (hasActiveCompost(client.farmingJson)) {
            return true
        }
        return hasActivePatches(client.farmingJson)
    }

    private fun hasPendingSaplings(client: Client): Boolean {
        for (item in client.playerItems) {
            if (item in waterSaplingIds) {
                return true
            }
        }
        for (item in client.bankItems) {
            if (item in waterSaplingIds) {
                return true
            }
        }
        return false
    }

    private fun alignedCycle(player: Client, currentCycle: Long, interval: Long): Long {
        val stableId = when {
            player.dbId > 0 -> player.dbId
            player.slot >= 0 -> player.slot
            else -> System.identityHashCode(player)
        }
        val offset = Math.floorMod(stableId, interval.toInt()).toLong()
        val base = (currentCycle / interval) * interval
        var target = base + offset
        if (target <= currentCycle) {
            target += interval
        }
        return target
    }

    private fun hasActiveCompost(farmingJson: FarmingState): Boolean {
        for (compost in FarmingDefinitions.compostBin.values()) {
            val value = farmingJson.getCompostData().get(compost.name)?.asJsonArray ?: continue
            val state = value.get(1).asString
            if (
                state.equals(FarmingDefinitions.compostState.CLOSED.toString(), true) ||
                state.equals(FarmingDefinitions.compostState.FILLED.toString(), true)
            ) {
                return true
            }
        }
        return false
    }

    private fun hasActivePatches(farmingJson: FarmingState): Boolean {
        for (patch in FarmingDefinitions.patches.values()) {
            val values = farmingJson.getPatchData().get(patch.name)?.asJsonArray ?: continue
            for (slot in 0 until patch.objectId.size) {
                val checkPos = slot * farmingJson.PATCHAMOUNT
                val itemId = values.get(checkPos).asInt
                val state = values.get(checkPos + 1).asString
                val stage = values.get(checkPos + 3).asInt
                if (itemId != -1) {
                    return true
                }
                if (!state.equals(FarmingDefinitions.patchState.WEED.toString(), true)) {
                    return true
                }
                if (stage > 0) {
                    return true
                }
            }
        }
        return false
    }

    companion object {
        @JvmField
        val INSTANCE = FarmingScheduler()

        private const val ACTIVE_INTERVAL_TICKS = GlobalPulseService.FIVE_MINUTE_PULSE_TICKS
        private const val IDLE_RECHECK_INTERVAL_TICKS = GlobalPulseService.FIVE_MINUTE_PULSE_TICKS
    }
}
