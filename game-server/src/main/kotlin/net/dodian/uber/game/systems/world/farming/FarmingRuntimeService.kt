package net.dodian.uber.game.systems.world.farming

import java.util.IdentityHashMap
import java.util.TreeMap
import kotlin.system.measureNanoTime
import net.dodian.uber.game.content.skills.farming.FarmingData
import net.dodian.uber.game.content.skills.farming.markFarmingDirty
import net.dodian.uber.game.engine.config.runtimePhaseWarnMs
import net.dodian.uber.game.engine.tasking.TaskHandle
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.player.PlayerSaveReason
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import net.dodian.uber.game.tasks.TickTasks
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import org.slf4j.LoggerFactory

class FarmingRuntimeService {
    private val logger = LoggerFactory.getLogger(FarmingRuntimeService::class.java)
    private val scheduledAt = IdentityHashMap<Client, Long>()
    private val dueBuckets = TreeMap<Long, MutableSet<Client>>()
    private val runtimeStateByPlayer = IdentityHashMap<Client, PlayerFarmingRuntimeState>()
    private val pendingSaveAt = IdentityHashMap<Client, Long>()
    private val deferredLoginCatchUpStartCycle = IdentityHashMap<Client, Long>()
    private val deferredLoginCatchUpLatencyByTicks = TreeMap<Long, Int>()
    @Volatile
    private var tickPilotHandle: TaskHandle? = null

    fun recordDeferredLoginCatchUpStart(player: Client, startCycle: Long) {
        deferredLoginCatchUpStartCycle[player] = startCycle
    }

    @Synchronized
    fun deferredLoginCatchUpLatencySnapshot(): Map<Long, Int> = deferredLoginCatchUpLatencyByTicks.toMap()

    fun onLogin(player: Client, nowMs: Long) {
        ensureTickPilotStarted()
        applyTimestampCatchUp(player, nowMs)
        notePlayer(player, nowMs, immediate = true)
    }

    fun onPatchInteraction(player: Client, nowMs: Long) {
        ensureTickPilotStarted()
        applyTimestampCatchUp(player, nowMs)
        notePlayer(player, nowMs, immediate = true)
    }

    fun onCompostInteraction(player: Client, nowMs: Long) {
        ensureTickPilotStarted()
        applyTimestampCatchUp(player, nowMs)
        notePlayer(player, nowMs, immediate = true)
    }

    fun onSaplingInventoryChange(player: Client, nowMs: Long) {
        ensureTickPilotStarted()
        notePlayer(player, nowMs, immediate = true)
    }

    fun onStateDirty(player: Client, nowMs: Long) {
        ensureTickPilotStarted()
        pendingSaveAt[player] = nowMs + FARMING_SAVE_DEBOUNCE_MS
        runtimeStateByPlayer[player]?.dirty = true
        notePlayer(player, nowMs, immediate = false)
    }

    @Synchronized
    fun ensureTickPilotStarted(): Boolean {
        val handle = tickPilotHandle
        if (handle != null && !handle.isCancelled()) {
            return false
        }
        tickPilotHandle =
            TickTasks.worldTaskCoroutine {
                repeatEvery(intervalTicks = FARMING_RUNTIME_POLL_TICKS) {
                    runTick(System.currentTimeMillis())
                    true
                }
            }
        logger.info("Farming runtime pilot bound to TickTasks world coroutine")
        return true
    }

    private fun runTick(nowMs: Long) {
        val runStats: FarmingRunStats
        val elapsed =
            measureNanoTime {
                runStats = runDue(nowMs)
            }
        val elapsedMs = elapsed / 1_000_000L
        if (elapsedMs >= runtimePhaseWarnMs) {
            logger.warn(
                "Farming runtime tick took {}ms duePlayers={} processedPlayers={} maxBucketSize={}",
                elapsedMs,
                runStats.duePlayers,
                runStats.processedPlayers,
                runStats.maxBucketSize,
            )
        }
    }

    fun runDue(nowMs: Long): FarmingRunStats {
        drainInvalidPlayers()
        flushDebouncedSaves(nowMs)

        var duePlayers = 0
        var processedPlayers = 0
        var maxBucketSize = 0

        while (dueBuckets.isNotEmpty()) {
            val first = dueBuckets.firstEntry()
            if (first.key > nowMs) {
                break
            }
            dueBuckets.pollFirstEntry()
            val players = first.value.toList()
            if (players.size > maxBucketSize) {
                maxBucketSize = players.size
            }
            duePlayers += players.size

            for (client in players) {
                scheduledAt.remove(client)
                if (!client.isActive || client.disconnected) {
                    runtimeStateByPlayer.remove(client)
                    pendingSaveAt.remove(client)
                    continue
                }
                applyTimestampCatchUp(client, nowMs)
                processedPlayers++
                notePlayer(client, nowMs, immediate = false)
            }
        }

        return FarmingRunStats(
            duePlayers = duePlayers,
            processedPlayers = processedPlayers,
            maxBucketSize = maxBucketSize,
        )
    }

    private fun notePlayer(player: Client, nowMs: Long, immediate: Boolean) {
        if (!player.isActive || player.disconnected) {
            unschedule(player)
            deferredLoginCatchUpStartCycle.remove(player)
            runtimeStateByPlayer.remove(player)
            return
        }

        val snapshot = FarmingPersistenceCodec.snapshot(player)
        if (!hasActiveWork(snapshot)) {
            unschedule(player)
            runtimeStateByPlayer[player] =
                PlayerFarmingRuntimeState(
                    nextDueAtMillis = Long.MAX_VALUE,
                    lastAppliedPulseAtMillis = player.farmingJson.lastGlobalPulseAtMillis,
                    dirty = false,
                    patchStates = mutableMapOf(),
                    compostStates = mutableMapOf(),
                    saplingState = null,
                )
            return
        }

        val nextDue = if (immediate) nowMs else nowMs + FARMING_PULSE_MS
        schedule(player, nextDue)
        runtimeStateByPlayer[player] = buildRuntimeState(player, snapshot, nextDue)
    }

    private fun applyTimestampCatchUp(player: Client, nowMs: Long) {
        val state = player.farmingJson
        val lastPulseAt = state.lastGlobalPulseAtMillis
        if (lastPulseAt <= 0L) {
            state.lastGlobalPulseAtMillis = nowMs
            player.markFarmingDirty()
            maybeRecordDeferredCatchUpLatency(player, nowMs)
            return
        }

        val elapsed = nowMs - lastPulseAt
        if (elapsed < FARMING_PULSE_MS) {
            maybeRecordDeferredCatchUpLatency(player, nowMs)
            return
        }

        val snapshot = FarmingPersistenceCodec.snapshot(player)
        val activeSlots =
            snapshot.patchSlots.count {
                it.itemId != -1 || !it.state.equals(FarmingData.patchState.WEED.toString(), true) || it.stageOrLife > 0
            }
        val activeBins =
            snapshot.compostBins.count {
                it.state.equals(FarmingData.compostState.CLOSED.toString(), true) ||
                    it.state.equals(FarmingData.compostState.FILLED.toString(), true)
            }
        val requiredWindows = (activeSlots + activeBins + if (snapshot.hasPendingSaplings) 1 else 0).coerceAtLeast(1)
        val rawPulses = (elapsed / FARMING_PULSE_MS).toInt()
        val pulsesToApply =
            rawPulses
                .coerceAtMost(requiredWindows.coerceAtMost(MAX_CATCH_UP_PULSES))
                .coerceAtMost(MAX_CATCH_UP_PULSES_PER_INVOCATION)

        if (pulsesToApply <= 0) {
            maybeRecordDeferredCatchUpLatency(player, nowMs)
            return
        }

        val elapsedNs = measureNanoTime {
            repeat(pulsesToApply) {
                player.farming.run { player.updateFarming() }
            }
        }

        state.lastGlobalPulseAtMillis =
            if (rawPulses > MAX_CATCH_UP_PULSES) {
                nowMs
            } else {
                lastPulseAt + (pulsesToApply * FARMING_PULSE_MS)
            }
        player.markFarmingDirty()

        if (elapsedNs / 1_000_000L >= 30L) {
            logger.warn(
                "Farming catch-up took {}ms for {} pulses player={} dbId={}",
                elapsedNs / 1_000_000L,
                pulsesToApply,
                player.playerName,
                player.dbId,
            )
        }
        maybeRecordDeferredCatchUpLatency(player, nowMs)
    }

    private fun maybeRecordDeferredCatchUpLatency(player: Client, nowMs: Long) {
        val startCycle = deferredLoginCatchUpStartCycle[player] ?: return
        val lagMs = nowMs - player.farmingJson.lastGlobalPulseAtMillis
        if (lagMs > FARMING_PULSE_MS) {
            return
        }
        deferredLoginCatchUpStartCycle.remove(player)
        val ticks = (TickTasks.gameClock() - startCycle).coerceAtLeast(0L)
        deferredLoginCatchUpLatencyByTicks[ticks] = (deferredLoginCatchUpLatencyByTicks[ticks] ?: 0) + 1
        logger.info(
            "Farming deferred login catch-up settled in {} ticks player={} dbId={} lagMs={}",
            ticks,
            player.playerName,
            player.dbId,
            lagMs,
        )
    }

    private fun flushDebouncedSaves(nowMs: Long) {
        if (pendingSaveAt.isEmpty()) {
            return
        }
        val iterator = pendingSaveAt.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val player = entry.key
            val dueAt = entry.value
            if (dueAt > nowMs) {
                continue
            }
            iterator.remove()
            if (!player.isActive || player.disconnected) {
                continue
            }
            if (player.saveDirtyMask and PlayerSaveSegment.FARMING.mask == 0) {
                continue
            }
            player.saveStats(PlayerSaveReason.PERIODIC, false, false)
            runtimeStateByPlayer[player]?.dirty = false
        }
    }

    private fun buildRuntimeState(
        player: Client,
        snapshot: PlayerFarmingSnapshot,
        nextDueAtMillis: Long,
    ): PlayerFarmingRuntimeState {
        val patchStates = mutableMapOf<String, PatchSlotRuntimeState>()
        snapshot.patchSlots.forEach { slot ->
            val key = "${slot.patch.name}:${slot.slot}"
            patchStates[key] =
                PatchSlotRuntimeState(
                    nextDueAtMillis = nextDueAtMillis,
                    lastAppliedPulseAtMillis = player.farmingJson.lastGlobalPulseAtMillis,
                    state = slot.state,
                    dirty = false,
                )
        }

        val compostStates = mutableMapOf<String, CompostBinRuntimeState>()
        snapshot.compostBins.forEach { bin ->
            compostStates[bin.bin.name] =
                CompostBinRuntimeState(
                    nextDueAtMillis = nextDueAtMillis,
                    lastAppliedPulseAtMillis = player.farmingJson.lastGlobalPulseAtMillis,
                    state = bin.state,
                    dirty = false,
                )
        }

        val saplingState =
            if (snapshot.hasPendingSaplings) {
                SaplingRuntimeState(
                    nextDueAtMillis = nextDueAtMillis,
                    lastAppliedPulseAtMillis = player.farmingJson.lastGlobalPulseAtMillis,
                    state = "PENDING",
                    dirty = false,
                )
            } else {
                null
            }

        return PlayerFarmingRuntimeState(
            nextDueAtMillis = nextDueAtMillis,
            lastAppliedPulseAtMillis = player.farmingJson.lastGlobalPulseAtMillis,
            dirty = false,
            patchStates = patchStates,
            compostStates = compostStates,
            saplingState = saplingState,
        )
    }

    private fun hasActiveWork(snapshot: PlayerFarmingSnapshot): Boolean {
        if (snapshot.hasPendingSaplings) {
            return true
        }
        if (
            snapshot.compostBins.any {
                it.state.equals(FarmingData.compostState.CLOSED.toString(), true) ||
                    it.state.equals(FarmingData.compostState.FILLED.toString(), true)
            }
        ) {
            return true
        }
        return snapshot.patchSlots.any {
            it.itemId != -1 ||
                !it.state.equals(FarmingData.patchState.WEED.toString(), true) ||
                it.stageOrLife > 0
        }
    }

    private fun schedule(player: Client, atMillis: Long) {
        unschedule(player)
        scheduledAt[player] = atMillis
        dueBuckets.computeIfAbsent(atMillis) { linkedSetOf() }.add(player)
    }

    private fun unschedule(player: Client) {
        val existing = scheduledAt.remove(player) ?: return
        val bucket = dueBuckets[existing] ?: return
        bucket.remove(player)
        if (bucket.isEmpty()) {
            dueBuckets.remove(existing)
        }
    }

    private fun drainInvalidPlayers() {
        val activeSet = IdentityHashMap<Client, Boolean>(PlayerRegistry.playersOnline.size)
        PlayerRegistry.forEachActivePlayer { activeSet[it] = true }

        val iterator = scheduledAt.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val client = entry.key
            if (activeSet.containsKey(client) && client.isActive && !client.disconnected) {
                continue
            }
            val due = entry.value
            val bucket = dueBuckets[due]
            bucket?.remove(client)
            if (bucket != null && bucket.isEmpty()) {
                dueBuckets.remove(due)
            }
            pendingSaveAt.remove(client)
            deferredLoginCatchUpStartCycle.remove(client)
            runtimeStateByPlayer.remove(client)
            iterator.remove()
        }
    }

    companion object {
        @JvmField
        val INSTANCE = FarmingRuntimeService()

        private const val FARMING_PULSE_MS = 300_000L
        private const val FARMING_RUNTIME_POLL_TICKS = 1
        private const val FARMING_SAVE_DEBOUNCE_MS = 5_000L
        private const val MAX_CATCH_UP_PULSES = 288
        private const val MAX_CATCH_UP_PULSES_PER_INVOCATION = 6
    }
}
