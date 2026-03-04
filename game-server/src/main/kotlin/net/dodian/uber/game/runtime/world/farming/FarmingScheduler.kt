package net.dodian.uber.game.runtime.world.farming

import com.google.gson.JsonArray
import java.util.IdentityHashMap
import java.util.TreeMap
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.FarmingData
import net.dodian.uber.game.skills.FarmingJson
import net.dodian.uber.game.model.entity.player.PlayerHandler

class FarmingScheduler {
    private val scheduled = IdentityHashMap<Client, Long>()
    private val buckets = TreeMap<Long, FarmingTickBucket>()

    fun refreshActivePlayers(
        activePlayers: List<Client>,
        currentCycle: Long,
    ) {
        val activeSet = IdentityHashMap<Client, Boolean>(activePlayers.size)
        activePlayers.forEach { client ->
            activeSet[client] = true
            if (!scheduled.containsKey(client)) {
                val delay = if (hasActiveWork(client)) ACTIVE_INTERVAL_TICKS else IDLE_RECHECK_INTERVAL_TICKS
                schedule(client, currentCycle + delay)
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
    }

    fun noteActivity(player: Client, currentCycle: Long = PlayerHandler.cycle.toLong()) {
        schedule(player, currentCycle + ACTIVE_INTERVAL_TICKS)
    }

    fun runDue(currentCycle: Long): Pair<Int, Int> {
        var dueCount = 0
        var processed = 0
        while (buckets.isNotEmpty()) {
            val entry = buckets.firstEntry()
            if (entry.key > currentCycle) {
                break
            }
            buckets.pollFirstEntry()
            val players = entry.value.duePlayers.drain()
            dueCount += players.size
            players.forEach { client ->
                scheduled.remove(client)
                if (!client.isActive || client.disconnected) {
                    return@forEach
                }
                client.farming.run { client.updateFarming() }
                processed++
                val delay = if (hasActiveWork(client)) ACTIVE_INTERVAL_TICKS else IDLE_RECHECK_INTERVAL_TICKS
                schedule(client, currentCycle + delay)
            }
        }
        return dueCount to processed
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
        val waterIds = FarmingData.sapling.values().map { it.waterId + 1 }.toSet()
        for (item in client.playerItems) {
            if (item in waterIds) {
                return true
            }
        }
        for (item in client.bankItems) {
            if (item in waterIds) {
                return true
            }
        }
        return false
    }

    private fun hasActiveCompost(farmingJson: FarmingJson): Boolean {
        for (compost in FarmingData.compostBin.values()) {
            val value = farmingJson.getCompostData().get(compost.name)?.asJsonArray ?: continue
            val state = value.get(1).asString
            if (
                state.equals(FarmingData.compostState.CLOSED.toString(), true) ||
                state.equals(FarmingData.compostState.FILLED.toString(), true)
            ) {
                return true
            }
        }
        return false
    }

    private fun hasActivePatches(farmingJson: FarmingJson): Boolean {
        for (patch in FarmingData.patches.values()) {
            val values = farmingJson.getPatchData().get(patch.name)?.asJsonArray ?: continue
            for (slot in 0 until patch.objectId.size) {
                val checkPos = slot * farmingJson.PATCHAMOUNT
                val itemId = values.get(checkPos).asInt
                val state = values.get(checkPos + 1).asString
                val stage = values.get(checkPos + 3).asInt
                if (itemId != -1) {
                    return true
                }
                if (!state.equals(FarmingData.patchState.WEED.toString(), true)) {
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

        private const val ACTIVE_INTERVAL_TICKS = 100L
        private const val IDLE_RECHECK_INTERVAL_TICKS = 1000L
    }
}
