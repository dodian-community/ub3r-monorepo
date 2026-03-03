package net.dodian.uber.game.runtime.sync.player.root

import io.netty.buffer.ByteBuf
import java.util.IdentityHashMap
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.entity.player.PlayerUpdating
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.netty.codec.MessageType
import net.dodian.uber.game.runtime.sync.SynchronizationContext
import net.dodian.uber.game.runtime.sync.player.viewport.PlayerInfoViewportIndex
import net.dodian.utilities.syncPlayerAdmissionQueueEnabled
import net.dodian.utilities.syncPlayerDesiredLocalsEnabled
import net.dodian.utilities.syncPlayerIncrementalAddsEnabled
import net.dodian.utilities.syncPlayerFragmentReuseEnabled
import net.dodian.utilities.syncPlayerSelfOnlyEnabled
import net.dodian.utilities.syncPlayerStateValidationEnabled

class RootPlayerInfoService {
    private val playerUpdating = PlayerUpdating.getInstance()
    private val viewerStates = IdentityHashMap<Player, ViewerPlayerInfoState>()
    private val planner = DesiredLocalSetPlanner()
    private val admissionQueue = LocalAdmissionQueue()
    private val validator = PlayerInfoStateValidator()
    private val fragmentCache = PlayerInfoFragmentCache()

    fun sync(activePlayers: List<Client>) {
        val cycle = SynchronizationContext.current() ?: return
        val rootCycle =
            RootPlayerInfoCycle(
                viewers = activePlayers,
                viewportIndex = PlayerInfoViewportIndex.build(activePlayers, cycle.viewportIndex),
                subjectStates = buildSubjectStates(activePlayers),
            )
        if (syncPlayerFragmentReuseEnabled) {
            fragmentCache.movement.clear()
        }
        pruneViewerStates(activePlayers)

        activePlayers.forEach { viewer ->
            if (viewer.timeOutCounter >= 84) {
                viewer.disconnected = true
                viewer.println_debug("\nRemove non-responding " + viewer.playerName + " after 60 seconds of disconnect! ")
            }
            if (viewer.disconnected) {
                viewer.println_debug("\nRemove disconnected player " + viewer.playerName)
                net.dodian.uber.game.Server.playerHandler.removePlayer(viewer)
                viewer.disconnected = false
                PlayerHandler.players[viewer.slot] = null
                return@forEach
            }

            val plan = buildPlan(viewer, rootCycle)
            recordPlanMetrics(plan)
            dispatch(viewer, plan)
            captureViewerState(viewer, rootCycle, plan)
        }
    }

    private fun buildPlan(viewer: Client, cycle: RootPlayerInfoCycle): RootPlayerInfoPlan {
        val state = viewerStates.computeIfAbsent(viewer) { ViewerPlayerInfoState() }
        val desiredState = state.desiredLocalState
        val currentLocalSlots = actualLocalSlots(viewer)
        val visibleSlots = cycle.viewportIndex.visibleSlots(viewer)
        val buildAreaSignature = buildAreaSignature(viewer)
        val selfMovementChanged = viewer.primaryDirection != -1 || viewer.secondaryDirection != -1
        val selfBlockChanged = viewer.updateFlags.isUpdateRequired
        val teleport = viewer.didTeleport()
        val mapRegionChanged = viewer.didMapRegionChange() ||
            (state.lastKnownRegionBaseX != Int.MIN_VALUE &&
                (state.lastKnownRegionBaseX != viewer.mapRegionX ||
                    state.lastKnownRegionBaseY != viewer.mapRegionY ||
                    state.lastKnownPlane != viewer.position.z))
        val buildAreaChanged = state.lastBuildAreaSignature != 0 && state.lastBuildAreaSignature != buildAreaSignature
        val recoveryReason =
            if (syncPlayerStateValidationEnabled && !teleport && !mapRegionChanged && !buildAreaChanged) {
                validator.validate(viewer, state)
            } else {
                null
            }

        val desiredLocalSet =
            if (syncPlayerDesiredLocalsEnabled) {
                planner.build(viewer, currentLocalSlots, visibleSlots, MAX_LOCAL_PLAYERS)
            } else {
                DesiredLocalSet(currentLocalSlots, currentLocalSlots.size, currentLocalSlots.contentHashCode(), currentLocalSlots.size >= MAX_LOCAL_PLAYERS)
            }
        val diff = planner.diff(currentLocalSlots, desiredLocalSet)
        val pendingCount =
            if (syncPlayerAdmissionQueueEnabled) {
                admissionQueue.rebuildPending(desiredState, diff, desiredLocalSet.signature)
            } else {
                diff.additions.size
            }
        val admissionBatch =
            if (syncPlayerIncrementalAddsEnabled) {
                admissionQueue.drainPending(desiredState, MAX_LOCAL_PLAYER_ADDS_PER_TICK)
            } else {
                LocalAdmissionBatch(IntArray(0), pendingCount, AdmissionProgressState(pendingCount, 0, pendingCount))
            }

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
            return fullRebuildPlan(PlayerPacketBuildReason.MAP_REGION_CHANGE, diff, desiredLocalSet, pendingCount, admissionBatch)
        }
        if (buildAreaChanged) {
            return fullRebuildPlan(PlayerPacketBuildReason.BUILD_AREA_CHANGE, diff, desiredLocalSet, pendingCount, admissionBatch)
        }

        if (pendingCount == 0 && diff.removals.isEmpty() && diff.changedRetained.isEmpty()) {
            if (!selfMovementChanged && !selfBlockChanged) {
                return RootPlayerInfoPlan(
                    mode = PlayerPacketMode.SKIP,
                    buildReason = null,
                    skipReason = PlayerPacketSkipReason.NO_CHANGES_PENDING_EMPTY,
                    diff = diff,
                    desiredLocalSet = desiredLocalSet,
                    pendingAddCount = 0,
                    actualAdditions = IntArray(0),
                    deferredAdditionCount = 0,
                    selfMovementChanged = false,
                    selfBlockChanged = false,
                )
            }
            if (syncPlayerSelfOnlyEnabled) {
                return RootPlayerInfoPlan(
                    mode = PlayerPacketMode.SELF_ONLY,
                    buildReason = if (selfBlockChanged) PlayerPacketBuildReason.SELF_BLOCK else PlayerPacketBuildReason.SELF_MOVEMENT,
                    skipReason = null,
                    diff = diff,
                    desiredLocalSet = desiredLocalSet,
                    pendingAddCount = 0,
                    actualAdditions = IntArray(0),
                    deferredAdditionCount = 0,
                    selfMovementChanged = selfMovementChanged,
                    selfBlockChanged = selfBlockChanged,
                )
            }
        }

        if (pendingCount > 0) {
            if (!syncPlayerIncrementalAddsEnabled) {
                return fullRebuildPlan(PlayerPacketBuildReason.LOCAL_ADMISSION_PENDING, diff, desiredLocalSet, pendingCount, admissionBatch)
            }
            return RootPlayerInfoPlan(
                mode = PlayerPacketMode.INCREMENTAL_ADMISSION,
                buildReason = PlayerPacketBuildReason.LOCAL_ADMISSION_PENDING,
                skipReason = null,
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
                    diff.removals.isNotEmpty() -> PlayerPacketBuildReason.LOCAL_REMOVAL
                    diff.changedRetained.isNotEmpty() -> PlayerPacketBuildReason.RETAINED_LOCAL_CHANGED
                    selfBlockChanged -> PlayerPacketBuildReason.SELF_BLOCK
                    selfMovementChanged -> PlayerPacketBuildReason.SELF_MOVEMENT
                    else -> PlayerPacketBuildReason.RETAINED_LOCAL_CHANGED
                },
            skipReason = null,
            diff = diff,
            desiredLocalSet = desiredLocalSet,
            pendingAddCount = 0,
            actualAdditions = IntArray(0),
            deferredAdditionCount = 0,
            selfMovementChanged = selfMovementChanged,
            selfBlockChanged = selfBlockChanged,
        )
    }

    private fun fullRebuildPlan(
        reason: PlayerPacketBuildReason,
        diff: DesiredLocalSetDiff,
        desiredLocalSet: DesiredLocalSet,
        pendingCount: Int,
        admissionBatch: LocalAdmissionBatch,
    ): RootPlayerInfoPlan =
        RootPlayerInfoPlan(
            mode = PlayerPacketMode.FULL_REBUILD,
            buildReason = reason,
            skipReason = null,
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
            PlayerPacketMode.SKIP -> SynchronizationContext.recordPlayerPacketSkipped(viewer.playerListSize)
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

    private fun sendPacket(viewer: Client, write: (ByteMessage) -> Unit) {
        val pooledBuffer: ByteBuf = ByteMessage.pooledBuffer(8192)
        var message: ByteMessage? = null
        try {
            message = ByteMessage.message(81, MessageType.VAR_SHORT, pooledBuffer)
            write(message)
            viewer.send(message)
        } catch (e: Exception) {
            if (message != null) {
                message.releaseAll()
            } else if (pooledBuffer.refCnt() > 0) {
                pooledBuffer.release(pooledBuffer.refCnt())
            }
            throw e
        }
    }

    private fun captureViewerState(viewer: Client, cycle: RootPlayerInfoCycle, plan: RootPlayerInfoPlan) {
        val state = viewerStates.computeIfAbsent(viewer) { ViewerPlayerInfoState() }
        val locals = actualLocalSlots(viewer)
        var movementStamp = 0L
        var blockStamp = 0L
        for (slot in locals) {
            val subjectState = cycle.subjectStates[slot] ?: continue
            movementStamp = maxOf(movementStamp, subjectState.movementRevision)
            blockStamp = maxOf(blockStamp, subjectState.blockRevision)
        }

        val desiredState = state.desiredLocalState
        desiredState.currentLocalSlots = locals
        desiredState.currentLocalCount = viewer.playerListSize
        desiredState.desiredLocalSlots = plan.desiredLocalSet.slots
        desiredState.desiredLocalCount = plan.desiredLocalSet.count
        desiredState.lastVisibleSignature = cycle.viewportIndex.visibleSignature(viewer)
        desiredState.lastRegionBaseX = viewer.mapRegionX
        desiredState.lastRegionBaseY = viewer.mapRegionY
        desiredState.lastPlane = viewer.position.z
        desiredState.lastPacketMode = plan.mode
        desiredState.needsHardRebuild = false

        state.lastKnownLocalSlots = locals
        state.lastKnownLocalCount = viewer.playerListSize
        state.lastKnownRegionBaseX = viewer.mapRegionX
        state.lastKnownRegionBaseY = viewer.mapRegionY
        state.lastKnownPlane = viewer.position.z
        state.lastVisibleSignature = cycle.viewportIndex.visibleSignature(viewer)
        state.lastBuildAreaSignature = buildAreaSignature(viewer)
        state.lastLocalMovementStamp = movementStamp
        state.lastLocalBlockStamp = blockStamp
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
        cycle.recordPlayerVisibleDiff(plan.diff.additions.size, plan.diff.removals.size)
        cycle.recordPlayerRetainedLocalChanged(plan.diff.changedRetained.size)
        cycle.recordPlayerDesiredLocals(plan.desiredLocalSet.count, plan.desiredLocalSet.isSaturated)
        cycle.recordPlayerPendingAdds(plan.pendingAddCount, plan.deferredAdditionCount)
        cycle.recordPlayerLocalRemovalCount(plan.diff.removals.size)
        cycle.recordPlayerLocalAdditionSent(plan.actualAdditions.size)
        cycle.recordPlayerLocalAdditionDeferred(plan.deferredAdditionCount)
        if (plan.recoveryReason != null) {
            cycle.recordPlayerRecovery(plan.recoveryReason)
        }
    }

    private fun buildSubjectStates(activePlayers: List<Client>): Map<Int, SubjectPlayerInfoState> {
        val states = HashMap<Int, SubjectPlayerInfoState>(activePlayers.size)
        activePlayers.forEach { player ->
            states[player.slot] =
                SubjectPlayerInfoState(
                    slot = player.slot,
                    movementRevision = SynchronizationContext.getPlayerMovementRevision(player),
                    blockRevision = SynchronizationContext.getPlayerBlockRevision(player),
                )
        }
        return states
    }

    private fun actualLocalSlots(viewer: Client): IntArray {
        val locals = IntArray(viewer.playerListSize)
        for (i in 0 until viewer.playerListSize) {
            locals[i] = viewer.playerList[i]?.slot ?: -1
        }
        return locals
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
    }
}
