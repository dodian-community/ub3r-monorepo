package net.dodian.uber.game.engine.sync.playerinfo

import net.dodian.uber.game.engine.sync.playerinfo.admission.*
import net.dodian.uber.game.engine.sync.playerinfo.dispatch.*
import net.dodian.uber.game.engine.sync.playerinfo.state.*

import io.netty.buffer.ByteBuf
import java.util.IdentityHashMap
import java.util.LinkedHashMap
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.entity.player.PlayerUpdating
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.netty.codec.MessageType
import net.dodian.uber.game.engine.sync.SynchronizationContext
import org.slf4j.LoggerFactory

class RootPlayerInfoService {
    private val logger = LoggerFactory.getLogger(RootPlayerInfoService::class.java)
    private val playerUpdating = PlayerUpdating.getInstance()
    private val viewerStates = IdentityHashMap<Player, ViewerPlayerInfoState>()
    private val idleTemplateCache =
        object : LinkedHashMap<IdlePlayerSyncTemplateKey, ByteArray>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<IdlePlayerSyncTemplateKey, ByteArray>?): Boolean =
                size > MAX_IDLE_TEMPLATE_CACHE_SIZE
        }
    private val planner = DesiredLocalSetPlanner()
    private val admissionQueue = LocalAdmissionQueue()
    private val validator = PlayerInfoStateValidator()
    private val fragmentCache = PlayerInfoFragmentCache()

    fun sync(activePlayers: List<Client>) {
        val cycle = SynchronizationContext.current() ?: return
        val rootCycle =
            RootPlayerInfoCycle(
                viewers = activePlayers,
                viewportIndex = cycle.viewportIndex,
            )
        pruneViewerStates(activePlayers)

        activePlayers.forEach { viewer ->
            try {
                if (viewer.timeOutCounter.get() >= 84) {
                    viewer.disconnected = true
                    viewer.println_debug("\nRemove non-responding " + viewer.playerName + " after 60 seconds of disconnect! ")
                }
                if (viewer.disconnected) {
                    viewer.println_debug("\nRemove disconnected player " + viewer.playerName)
                    PlayerRegistry.removePlayer(viewer)
                    viewer.disconnected = false
                    PlayerRegistry.players[viewer.slot] = null
                    return@forEach
                }

                val plan = buildPlan(viewer, rootCycle)
                recordPlanMetrics(plan)
                dispatch(viewer, plan)
                captureViewerState(viewer, plan)
            } catch (throwable: Throwable) {
                logger.error(
                    "Root player sync failed viewer={} slot={} pos={}",
                    viewer.playerName,
                    viewer.slot,
                    viewer.position,
                    throwable,
                )
                viewer.disconnected = true
            }
        }
    }

    private fun buildPlan(viewer: Client, cycle: RootPlayerInfoCycle): RootPlayerInfoPlan {
        val state = viewerStates.computeIfAbsent(viewer) { ViewerPlayerInfoState() }
        val desiredState = state.desiredLocalState
        val buildAreaSignature = buildAreaSignature(viewer)
        val selfMovementChanged = viewer.primaryDirection != -1 || viewer.secondaryDirection != -1
        val selfBlockChanged = viewer.updateFlags.isUpdateRequired
        val teleport = viewer.didTeleport()
        val currentChunkStamp = SynchronizationContext.getPlayerChunkActivityStamp(viewer)
        val currentMembershipRevision = viewer.localPlayerMembershipRevision
        val mapRegionChanged = viewer.didMapRegionChange() ||
            (state.lastKnownRegionBaseX != Int.MIN_VALUE &&
                (state.lastKnownRegionBaseX != viewer.mapRegionX ||
                    state.lastKnownRegionBaseY != viewer.mapRegionY ||
                    state.lastKnownPlane != viewer.position.z))
        val buildAreaChanged = state.lastBuildAreaSignature != 0 && state.lastBuildAreaSignature != buildAreaSignature

        val saturated = viewer.playerListSize >= MAX_LOCAL_PLAYERS

        // Fast-skip gate: do not allocate/copy local slot arrays or run the planner when we can prove nothing changed.
        // In overcrowded saturated local lists, visible-set churn does not matter (retain-first semantics),
        // so we only treat visible signature changes as a reason to rebuild when the viewer has free local slots.
        if (!teleport &&
            !mapRegionChanged &&
            !buildAreaChanged &&
            !state.needsHardRebuild &&
            !desiredState.needsHardRebuild &&
            desiredState.pendingAddCount == 0 &&
            !selfMovementChanged &&
            !selfBlockChanged &&
            currentChunkStamp == state.lastChunkActivityStamp &&
            currentMembershipRevision == state.lastLocalMembershipRevision
        ) {
            val visibleSignature =
                if (saturated) {
                    state.lastVisibleSignature
                } else {
                    computeVisibleSignature(viewer, candidatePlayers(viewer, cycle))
                }
            if (saturated || visibleSignature == state.lastVisibleSignature) {
                return RootPlayerInfoPlan(
                    mode = PlayerPacketMode.SKIP,
                    buildReason = null,
                    skipReason = PlayerPacketSkipReason.NO_CHANGES_PENDING_EMPTY,
                    visibleSignature = visibleSignature,
                    diff = EMPTY_DIFF,
                    desiredLocalSet = DesiredLocalSet(EMPTY_INT_ARRAY, viewer.playerListSize, 0, saturated),
                    pendingAddCount = 0,
                    actualAdditions = EMPTY_INT_ARRAY,
                    deferredAdditionCount = 0,
                    selfMovementChanged = false,
                    selfBlockChanged = false,
                )
            }
        }

        val recoveryReason =
            if (!teleport && !mapRegionChanged && !buildAreaChanged) {
                validator.validate(viewer, state)
            } else {
                null
            }

        // Build-required path: materialize current locals into a scratch array and plan desired locals/diffs.
        val currentLocalCount = copyCurrentLocals(viewer, desiredState.currentLocalSlots)
        desiredState.currentLocalCount = currentLocalCount
        val candidates = candidatePlayers(viewer, cycle)
        val desiredLocalSet = planner.build(viewer, desiredState.currentLocalSlots, currentLocalCount, candidates, MAX_LOCAL_PLAYERS)
        val diff = planner.diff(desiredState.currentLocalSlots, currentLocalCount, desiredLocalSet)
        val queueSignature = admissionQueueSignature(desiredLocalSet.signature, diff)
        val pendingCount = admissionQueue.rebuildPending(desiredState, diff, queueSignature)
        val admissionBatch = admissionQueue.drainPending(desiredState, MAX_LOCAL_PLAYER_ADDS_PER_TICK)
        val visibleSignature = computeVisibleSignature(viewer, candidates)

        if (recoveryReason != null) {
            state.needsHardRebuild = true
        }

        if (!viewer.loaded || state.needsHardRebuild || desiredState.needsHardRebuild || teleport || recoveryReason != null) {
            return RootPlayerInfoPlan(
                mode = PlayerPacketMode.FULL_REBUILD,
                buildReason =
                    when {
                        recoveryReason != null -> PlayerPacketBuildReason.STATE_MISMATCH_RECOVERY
                        teleport -> PlayerPacketBuildReason.TELEPORT
                        else -> PlayerPacketBuildReason.INITIAL_SYNC
                    },
                skipReason = null,
                visibleSignature = visibleSignature,
                diff = diff,
                desiredLocalSet = desiredLocalSet,
                pendingAddCount = pendingCount,
                actualAdditions = admissionBatch.sentSlots,
                deferredAdditionCount = admissionBatch.pendingCount,
                selfMovementChanged = selfMovementChanged,
                selfBlockChanged = selfBlockChanged,
                recoveryReason = recoveryReason,
            )
        }

        if (mapRegionChanged) {
            return fullRebuildPlan(PlayerPacketBuildReason.MAP_REGION_CHANGE, visibleSignature, diff, desiredLocalSet, pendingCount, admissionBatch)
        }
        if (buildAreaChanged) {
            return fullRebuildPlan(PlayerPacketBuildReason.BUILD_AREA_CHANGE, visibleSignature, diff, desiredLocalSet, pendingCount, admissionBatch)
        }

        if (pendingCount == 0 && diff.removalsCount == 0 && diff.changedRetainedCount == 0) {
            if (!selfMovementChanged && !selfBlockChanged) {
                return RootPlayerInfoPlan(
                    mode = PlayerPacketMode.SKIP,
                    buildReason = null,
                    skipReason = PlayerPacketSkipReason.NO_CHANGES_PENDING_EMPTY,
                    visibleSignature = visibleSignature,
                    diff = diff,
                    desiredLocalSet = desiredLocalSet,
                    pendingAddCount = 0,
                    actualAdditions = EMPTY_INT_ARRAY,
                    deferredAdditionCount = 0,
                    selfMovementChanged = false,
                    selfBlockChanged = false,
                )
            }
            return RootPlayerInfoPlan(
                mode = PlayerPacketMode.SELF_ONLY,
                buildReason = if (selfBlockChanged) PlayerPacketBuildReason.SELF_BLOCK else PlayerPacketBuildReason.SELF_MOVEMENT,
                skipReason = null,
                visibleSignature = visibleSignature,
                diff = diff,
                desiredLocalSet = desiredLocalSet,
                pendingAddCount = 0,
                actualAdditions = EMPTY_INT_ARRAY,
                deferredAdditionCount = 0,
                selfMovementChanged = selfMovementChanged,
                selfBlockChanged = selfBlockChanged,
            )
        }

        if (pendingCount > 0) {
            return RootPlayerInfoPlan(
                mode = PlayerPacketMode.INCREMENTAL_ADMISSION,
                buildReason = PlayerPacketBuildReason.LOCAL_ADMISSION_PENDING,
                skipReason = null,
                visibleSignature = visibleSignature,
                diff = diff,
                desiredLocalSet = desiredLocalSet,
                pendingAddCount = pendingCount,
                actualAdditions = admissionBatch.sentSlots,
                deferredAdditionCount = admissionBatch.pendingCount,
                selfMovementChanged = selfMovementChanged,
                selfBlockChanged = selfBlockChanged,
            )
        }

        return RootPlayerInfoPlan(
            mode = PlayerPacketMode.INCREMENTAL_STEADY,
            buildReason =
                when {
                    diff.removalsCount > 0 -> PlayerPacketBuildReason.LOCAL_REMOVAL
                    diff.changedRetainedCount > 0 -> PlayerPacketBuildReason.RETAINED_LOCAL_CHANGED
                    selfBlockChanged -> PlayerPacketBuildReason.SELF_BLOCK
                    selfMovementChanged -> PlayerPacketBuildReason.SELF_MOVEMENT
                    else -> PlayerPacketBuildReason.RETAINED_LOCAL_CHANGED
                },
            skipReason = null,
            diff = diff,
            desiredLocalSet = desiredLocalSet,
            pendingAddCount = 0,
            actualAdditions = EMPTY_INT_ARRAY,
            deferredAdditionCount = 0,
            selfMovementChanged = selfMovementChanged,
            selfBlockChanged = selfBlockChanged,
            visibleSignature = visibleSignature,
        )
    }

    private fun fullRebuildPlan(
        reason: PlayerPacketBuildReason,
        visibleSignature: Int,
        diff: DesiredLocalSetDiff,
        desiredLocalSet: DesiredLocalSet,
        pendingCount: Int,
        admissionBatch: LocalAdmissionBatch,
    ): RootPlayerInfoPlan =
        RootPlayerInfoPlan(
            mode = PlayerPacketMode.FULL_REBUILD,
            buildReason = reason,
            skipReason = null,
            visibleSignature = visibleSignature,
            diff = diff,
            desiredLocalSet = desiredLocalSet,
            pendingAddCount = pendingCount,
            actualAdditions = admissionBatch.sentSlots,
            deferredAdditionCount = admissionBatch.pendingCount,
            selfMovementChanged = false,
            selfBlockChanged = false,
        )

    private fun dispatch(viewer: Client, plan: RootPlayerInfoPlan) {
        when (plan.mode) {
            PlayerPacketMode.SKIP -> {
                sendIdleTemplate(viewer)
                SynchronizationContext.recordPlayerPacketIdleTemplated(viewer.playerListSize)
            }
            PlayerPacketMode.SELF_ONLY -> {
                sendPacket(viewer) { out -> playerUpdating.writeSelfOnlyUpdate(viewer, out, plan) }
                SynchronizationContext.recordPlayerPacketBuilt(viewer.playerListSize)
            }
            PlayerPacketMode.INCREMENTAL_STEADY -> {
                sendPacket(viewer) { out -> playerUpdating.writeIncrementalSteadyUpdate(viewer, out, plan) }
                SynchronizationContext.recordPlayerPacketBuilt(viewer.playerListSize)
            }
            PlayerPacketMode.INCREMENTAL_ADMISSION -> {
                sendPacket(viewer) { out -> playerUpdating.writeIncrementalAdmissionUpdate(viewer, out, plan) }
                SynchronizationContext.recordPlayerPacketBuilt(viewer.playerListSize)
            }
            PlayerPacketMode.FULL_REBUILD -> sendPacket(viewer) { out -> playerUpdating.writeFullRebuild(viewer, out, plan) }
        }
    }

    private fun sendIdleTemplate(viewer: Client) {
        playerUpdating.sendServerUpdateIfNeeded(viewer)
        val payload = idleTemplatePayload(viewer)
        viewer.noteIdlePlayerSyncSent()
        sendPacket(viewer, payload)
    }

    private fun sendPacket(viewer: Client, write: (ByteMessage) -> Unit) {
        val capacity = viewer.playerUpdateCapacity
        val pooledBuffer: ByteBuf = viewer.channel?.let { channel ->
            if (channel.isActive) {
                channel.alloc().buffer(capacity)
            } else {
                null
            }
        } ?: ByteMessage.pooledBuffer(capacity)
        var message: ByteMessage? = null
        try {
            message = ByteMessage.message(81, MessageType.VAR_SHORT, pooledBuffer)
            write(message)
            viewer.updatePlayerUpdateCapacity(message.content().writerIndex())
            viewer.send(message)
        } catch (e: RuntimeException) {
            if (message != null) {
                message.releaseAll()
            } else if (pooledBuffer.refCnt() > 0) {
                pooledBuffer.release(pooledBuffer.refCnt())
            }
            throw e
        }
    }

    private fun sendPacket(viewer: Client, payload: ByteArray) {
        val capacity = maxOf(viewer.playerUpdateCapacity, payload.size)
        val pooledBuffer: ByteBuf = viewer.channel?.let { channel ->
            if (channel.isActive) {
                channel.alloc().buffer(capacity)
            } else {
                null
            }
        } ?: ByteMessage.pooledBuffer(capacity)
        var message: ByteMessage? = null
        try {
            message = ByteMessage.message(81, MessageType.VAR_SHORT, pooledBuffer)
            message.putBytes(payload)
            viewer.updatePlayerUpdateCapacity(message.content().writerIndex())
            viewer.send(message)
        } catch (e: RuntimeException) {
            if (message != null) {
                message.releaseAll()
            } else if (pooledBuffer.refCnt() > 0) {
                pooledBuffer.release(pooledBuffer.refCnt())
            }
            throw e
        }
    }

    private fun idleTemplatePayload(viewer: Client): ByteArray {
        val state = viewerStates.computeIfAbsent(viewer) { ViewerPlayerInfoState() }
        val key =
            IdlePlayerSyncTemplateKey(
                localCount = viewer.playerListSize,
                localSignature = state.lastIdleTemplateSignature,
                mapRegionChange = false,
                serverUpdateRunning = Server.updateRunning,
            )
        return idleTemplateCache.getOrPut(key) {
            buildIdleTemplatePayload(key.localCount)
        }
    }

    private fun buildIdleTemplatePayload(localCount: Int): ByteArray {
        val stream = ByteMessage.raw(maxOf(32, localCount + 8))
        try {
            stream.startBitAccess()
            stream.putBits(1, 0)
            stream.putBits(8, localCount)
            repeat(localCount) {
                stream.putBits(1, 0)
            }
            stream.putBits(11, 2047)
            stream.endBitAccess()
            return stream.toByteArray()
        } finally {
            stream.releaseAll()
        }
    }

    private fun captureViewerState(viewer: Client, plan: RootPlayerInfoPlan) {
        val state = viewerStates.computeIfAbsent(viewer) { ViewerPlayerInfoState() }
        val desiredState = state.desiredLocalState

        // On SKIP, avoid copying local slot arrays. We still need to advance signatures/stamps so the next
        // tick can fast-skip without re-planning.
        if (plan.mode != PlayerPacketMode.SKIP) {
            val localCount = copyCurrentLocals(viewer, desiredState.currentLocalSlots)
            desiredState.currentLocalCount = localCount
            if (desiredState.desiredLocalSlots.size < MAX_LOCAL_PLAYERS) {
                desiredState.desiredLocalSlots = desiredState.desiredLocalSlots.copyOf(MAX_LOCAL_PLAYERS)
            }
            if (plan.desiredLocalSet.count > 0) {
                System.arraycopy(plan.desiredLocalSet.slots, 0, desiredState.desiredLocalSlots, 0, plan.desiredLocalSet.count)
            }
            desiredState.desiredLocalCount = plan.desiredLocalSet.count
        } else {
            desiredState.currentLocalCount = viewer.playerListSize
        }

        desiredState.lastVisibleSignature = plan.visibleSignature
        desiredState.lastRegionBaseX = viewer.mapRegionX
        desiredState.lastRegionBaseY = viewer.mapRegionY
        desiredState.lastPlane = viewer.position.z
        desiredState.lastPacketMode = plan.mode
        desiredState.needsHardRebuild = false

        state.lastKnownLocalCount = viewer.playerListSize
        state.lastIdleTemplateSignature = localSignature(desiredState.currentLocalSlots, desiredState.currentLocalCount)
        state.lastKnownRegionBaseX = viewer.mapRegionX
        state.lastKnownRegionBaseY = viewer.mapRegionY
        state.lastKnownPlane = viewer.position.z
        state.lastVisibleSignature = plan.visibleSignature
        state.lastBuildAreaSignature = buildAreaSignature(viewer)
        state.lastChunkActivityStamp = SynchronizationContext.getPlayerChunkActivityStamp(viewer)
        state.lastLocalMembershipRevision = viewer.localPlayerMembershipRevision
        state.lastPacketMode = plan.mode
        state.needsHardRebuild = false
        state.buildAreaState.regionBaseX = viewer.mapRegionX
        state.buildAreaState.regionBaseY = viewer.mapRegionY
        state.buildAreaState.plane = viewer.position.z
        state.buildAreaState.visibleSignature = state.lastVisibleSignature
        state.buildAreaState.forcedRebuild = false
    }

    private fun recordPlanMetrics(plan: RootPlayerInfoPlan) {
        val cycle = SynchronizationContext.current() ?: return
        cycle.recordPlayerPacketMode(plan.mode)
        plan.buildReason?.let { cycle.recordPlayerBuildReason(it) }
        plan.skipReason?.let { cycle.recordPlayerSkipReason(it) }
        cycle.recordPlayerVisibleDiff(plan.diff.additionsCount, plan.diff.removalsCount)
        cycle.recordPlayerRetainedLocalChanged(plan.diff.changedRetainedCount)
        cycle.recordPlayerDesiredLocals(plan.desiredLocalSet.count, plan.desiredLocalSet.isSaturated)
        cycle.recordPlayerPendingAdds(plan.pendingAddCount, plan.deferredAdditionCount)
        cycle.recordPlayerLocalRemovalCount(plan.diff.removalsCount)
        cycle.recordPlayerLocalAdditionSent(plan.actualAdditions.size)
        cycle.recordPlayerLocalAdditionDeferred(plan.deferredAdditionCount)
        cycle.recordPlayerTeleportReinserts(
            total = plan.diff.reinsertsCount,
            sent = minOf(plan.diff.reinsertsCount, plan.actualAdditions.size),
            deferred = (plan.diff.reinsertsCount - minOf(plan.diff.reinsertsCount, plan.actualAdditions.size)).coerceAtLeast(0),
        )
        if (plan.recoveryReason != null) {
            cycle.recordPlayerRecovery(plan.recoveryReason)
        }
    }

    private fun candidatePlayers(viewer: Client, cycle: RootPlayerInfoCycle): List<Player> {
        val snapshotPlayers = cycle.viewportIndex?.snapshotFor(viewer)?.players
        // Viewport snapshots are an optimization. If unavailable/empty during lifecycle transitions,
        // fall back to the active player list so visibility never collapses.
        return if (snapshotPlayers.isNullOrEmpty()) {
            cycle.viewers
        } else {
            snapshotPlayers
        }
    }

    private fun computeVisibleSignature(viewer: Client, candidates: Iterable<Player>): Int {
        var signature = 1
        for (other in candidates) {
            if (!PlayerVisibilityRules.isVisibleTo(viewer, other)) {
                continue
            }
            signature = 31 * signature + other.slot
        }
        return signature
    }

    private fun copyCurrentLocals(viewer: Client, out: IntArray): Int {
        val count = viewer.playerListSize.coerceAtMost(out.size)
        for (i in 0 until count) {
            out[i] = viewer.playerList[i]?.slot ?: -1
        }
        return count
    }

    private fun localSignature(slots: IntArray, count: Int): Int {
        var signature = 1
        for (i in 0 until count.coerceAtMost(slots.size)) {
            signature = 31 * signature + slots[i]
        }
        return signature
    }

    private fun admissionQueueSignature(
        desiredSignature: Int,
        diff: DesiredLocalSetDiff,
    ): Int {
        var signature = 31 * desiredSignature + diff.reinsertsCount
        for (index in 0 until diff.reinsertsCount) {
            signature = 31 * signature + diff.reinserts[index]
        }
        signature = 31 * signature + diff.additionsCount
        return signature
    }

    private fun pruneViewerStates(activePlayers: List<Client>) {
        val activeSet = IdentityHashMap<Player, Boolean>(activePlayers.size)
        activePlayers.forEach { activeSet[it] = true }
        val iterator = viewerStates.keys.iterator()
        while (iterator.hasNext()) {
            if (!activeSet.containsKey(iterator.next())) {
                iterator.remove()
            }
        }
    }

    private fun buildAreaSignature(viewer: Player): Int =
        ((viewer.mapRegionX and 0xFFFF) shl 16) xor
            (viewer.mapRegionY and 0xFFFF) xor
            ((viewer.position.z and 0x3) shl 30)

    companion object {
        @JvmField
        val INSTANCE = RootPlayerInfoService()

        private const val MAX_LOCAL_PLAYERS = 255
        private const val MAX_LOCAL_PLAYER_ADDS_PER_TICK = 15
        private const val MAX_IDLE_TEMPLATE_CACHE_SIZE = 1024

        private val EMPTY_INT_ARRAY = IntArray(0)
        private val EMPTY_DIFF =
            DesiredLocalSetDiff(
                removals = EMPTY_INT_ARRAY,
                removalsCount = 0,
                retains = EMPTY_INT_ARRAY,
                retainsCount = 0,
                reinserts = EMPTY_INT_ARRAY,
                reinsertsCount = 0,
                additions = EMPTY_INT_ARRAY,
                additionsCount = 0,
                changedRetained = EMPTY_INT_ARRAY,
                changedRetainedCount = 0,
                desiredCount = 0,
                desiredSaturated = false,
            )
    }
}

private data class IdlePlayerSyncTemplateKey(
    val localCount: Int,
    val localSignature: Int,
    val mapRegionChange: Boolean,
    val serverUpdateRunning: Boolean,
)
