package net.dodian.uber.game.engine.sync.player

import io.netty.buffer.ByteBuf
import java.util.Arrays
import net.dodian.uber.game.engine.config.gameMaxPlayers
import net.dodian.uber.game.engine.metrics.OperationalTelemetry
import net.dodian.uber.game.engine.sync.SynchronizationContext
import net.dodian.uber.game.engine.sync.protocol.PackedUpdateBlock
import net.dodian.uber.game.engine.sync.player.PlayerVisibilityRules
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.entity.player.PlayerUpdating
import net.dodian.uber.game.model.entity.player.TarnishAppearanceValidator
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.netty.codec.MessageType
import net.dodian.uber.game.engine.sync.util.SlotBitSet
import org.slf4j.LoggerFactory

/**
 * Protocol-317 player synchronization with Kronos-style staged ownership:
 * plan, encode, enqueue, then commit the viewer's next local state.
 */
class StagedPlayerSynchronizationService {
    private val updating = PlayerUpdating.getInstance()
    private val scratch = ThreadLocal.withInitial(::Scratch)

    fun synchronize(viewer: Client): Result {
        updating.sendServerUpdateIfNeeded(viewer)
        updating.prepareViewerSynchronization(viewer)
        val result = scratch.get().result
        if (canFastSkip(viewer)) {
            val localCount = viewer.playerListSize
            val capacity = maxOf(1024, viewer.playerUpdateCapacity)
            val pooledBuffer: ByteBuf = viewer.channel?.alloc()?.buffer(capacity) ?: ByteMessage.pooledBuffer(capacity)
            var message: ByteMessage? = null
            try {
                message = ByteMessage.message(81, MessageType.VAR_SHORT, pooledBuffer)
                encodeIdleFastPath(viewer, localCount, message)
                val packetBytes = message.content().writerIndex()
                viewer.updatePlayerUpdateCapacity(packetBytes)
                OperationalTelemetry.incrementCounter("sync.player.packet_bytes", packetBytes.toLong())
                OperationalTelemetry.incrementCounter("sync.player.fast_skipped")
                val accepted = viewer.sendRequiredSynchronization(message)
                message = null
                if (!accepted) {
                    OperationalTelemetry.incrementCounter("sync.player.required_enqueue_rejected")
                    return result.set(false, packetBytes, localCount, localCount, 0)
                }
                return result.set(true, packetBytes, localCount, localCount, 0)
            } finally {
                message?.releaseAll()
            }
        }

        val plan = buildPlan(viewer, scratch.get())
        val capacity = maxOf(1024, viewer.playerUpdateCapacity)
        val pooledBuffer: ByteBuf = viewer.channel?.alloc()?.buffer(capacity) ?: ByteMessage.pooledBuffer(capacity)
        var message: ByteMessage? = null
        try {
            message = ByteMessage.message(81, MessageType.VAR_SHORT, pooledBuffer)
            encode(viewer, plan, message)
            val packetBytes = message.content().writerIndex()
            if (logger.isDebugEnabled) {
                logger.debug(
                    "tarnish_player_update viewer={} slot={} bytes={} hash={}",
                    viewer.playerName,
                    viewer.slot,
                    packetBytes,
                    TarnishAppearanceValidator.hash(message.content()),
                )
            }
            require(packetBytes <= MAX_VAR_SHORT_PAYLOAD) {
                "Player synchronization payload exceeds protocol limit: $packetBytes"
            }
            viewer.updatePlayerUpdateCapacity(packetBytes)
            OperationalTelemetry.incrementCounter("sync.player.packet_bytes", packetBytes.toLong())
            OperationalTelemetry.incrementCounter("sync.player.planned_additions", plan.additionCount.toLong())
            val accepted = viewer.sendRequiredSynchronization(message)
            message = null // required send consumes ownership on success and rejection
            if (!accepted) {
                OperationalTelemetry.incrementCounter("sync.player.required_enqueue_rejected")
                return result.set(false, packetBytes, plan.previousCount, plan.nextCount, plan.additionCount)
            }
            commit(viewer, plan)
            OperationalTelemetry.incrementCounter("sync.player.staged_committed")
            OperationalTelemetry.incrementCounter("sync.player.committed_locals", plan.nextCount.toLong())
            return result.set(true, packetBytes, plan.previousCount, plan.nextCount, plan.additionCount)
        } finally {
            message?.releaseAll()
        }
    }

    internal fun buildPlan(viewer: Client, reusable: Scratch = scratch.get()): Plan {
        reusable.reset()
        val plan = reusable.plan
        if (viewer.loaded && !viewer.didTeleport()) {
            plan.previousCount = viewer.playerListSize.coerceIn(0, minOf(MAX_LOCALS, viewer.playerList.size))
            for (index in 0 until plan.previousCount) {
                val local = viewer.playerList[index]
                plan.previous[index] = local
                val retained = isRetained(viewer, local)
                plan.retainPrevious[index] = retained
                if (retained && local != null) {
                    plan.addNext(local)
                    reusable.markMember(local.slot)
                }
            }
        }

        val candidates = SynchronizationContext.getViewportSnapshot(viewer)?.players
        if (candidates != null) {
            candidates.forEach { candidate -> reusable.offerCandidate(viewer, candidate, plan) }
        } else {
            PlayerRegistry.players.forEach { candidate -> reusable.offerCandidate(viewer, candidate, plan) }
        }

        var estimatedBytes = BASE_PACKET_BUDGET
        for (index in 0 until reusable.candidateCount) {
            if (plan.nextCount >= minOf(MAX_LOCALS, viewer.playerList.size)) break
            val candidate = reusable.candidates[index] ?: continue
            val sharedBlock = SynchronizationContext.getSharedPlayerBlock(candidate, "ADD_LOCAL")
            val additionBytes =
                ADD_LOCAL_BIT_BYTES +
                    (sharedBlock?.estimatedWireBytes(true) ?: MAX_APPEARANCE_BLOCK_ESTIMATE)
            if (estimatedBytes + additionBytes > MAX_STAGED_PAYLOAD_BUDGET) break
            estimatedBytes += additionBytes
            plan.addAddition(candidate)
            reusable.markMember(candidate.slot)
        }
        plan.freeze()
        return plan
    }

    internal fun encode(viewer: Client, plan: Plan, stream: ByteMessage) {
        plan.requireFrozen()
        plan.resetUpdateBlocks()
        val hasSelfBlock = updating.hasSelfUpdate(viewer)
        updating.updateLocalPlayerMovement(viewer, stream, hasSelfBlock)
        if (hasSelfBlock) {
            plan.queueUpdate(requireNotNull(updating.buildSharedBlock(viewer, "UPDATE_SELF")))
        }
        stream.putBits(8, plan.previousCount)
        for (index in 0 until plan.previousCount) {
            val local = plan.previous[index]
            if (!plan.retainPrevious[index] || local == null) {
                updating.writeLocalRemoval(stream)
            } else {
                val movement = SynchronizationContext.getSharedPlayerMovement(local)
                if (movement != null) {
                    stream.putBitSlice(movement.bytes, 0, movement.bitCount)
                } else {
                    local.updatePlayerMovement(stream)
                }
                if (local.updateFlags.isUpdateRequired) {
                    val sharedBlock = SynchronizationContext.getSharedPlayerBlock(local, "UPDATE_LOCAL")
                    SynchronizationContext.recordPlayerBlockCacheHit(sharedBlock != null)
                    plan.queueUpdate(sharedBlock ?: requireNotNull(updating.buildSharedBlock(local, "UPDATE_LOCAL")))
                }
            }
        }
        for (index in 0 until plan.additionCount) {
            val addition = plan.additions[index]!!
            if (viewer.hasSeenCurrentAppearance(addition)) {
                val sharedUpdate = SynchronizationContext.getSharedPlayerBlock(addition, "UPDATE_LOCAL")
                val hasBlock = addition.updateFlags.isUpdateRequired
                updating.writeStagedLocalAddWithoutAppearance(viewer, addition, stream, hasBlock)
                if (hasBlock) {
                    SynchronizationContext.recordPlayerBlockCacheHit(sharedUpdate != null)
                    plan.queueUpdate(sharedUpdate ?: requireNotNull(updating.buildSharedBlock(addition, "UPDATE_LOCAL")))
                }
            } else {
                val sharedBlock = SynchronizationContext.getSharedPlayerBlock(addition, "ADD_LOCAL")
                SynchronizationContext.recordPlayerBlockCacheHit(sharedBlock != null)
                val block = sharedBlock ?: requireNotNull(updating.buildSharedBlock(addition, "ADD_LOCAL"))
                updating.writeStagedLocalAdd(viewer, addition, stream, true)
                plan.queueUpdate(block)
            }
            SynchronizationContext.recordPlayerAdd()
        }
        stream.putBits(11, PlayerUpdating.LOCAL_LIST_TERMINATOR)
        stream.endBitAccess()
        writePackedUpdatePlanes(plan, stream)
    }

    private fun writePackedUpdatePlanes(plan: Plan, stream: ByteMessage) {
        if (plan.updateBlockCount == 0) return
        for (index in 0 until plan.updateBlockCount) {
            val block = requireNotNull(plan.updateBlocks[index])
            var mask = block.mask
            if (mask >= 0x100) {
                mask = mask or 0x40
                stream.put(mask and 0xFF)
                stream.put(mask ushr 8)
            } else {
                stream.put(mask)
            }
        }
        stream.startBitAccess()
        for (index in 0 until plan.updateBlockCount) {
            val block = requireNotNull(plan.updateBlocks[index])
            stream.putBitSlice(block.fixedBytes, 0, block.fixedBitCount)
        }
        stream.endBitAccess()
        for (index in 0 until plan.updateBlockCount) {
            stream.putBytes(requireNotNull(plan.updateBlocks[index]).variableBytes)
        }
    }

    internal fun commit(viewer: Client, plan: Plan) {
        plan.requireFrozen()
        var changed = viewer.playerListSize != plan.nextCount
        if (!changed) {
            for (index in 0 until plan.nextCount) {
                if (viewer.playerList[index] !== plan.next[index]) {
                    changed = true
                    break
                }
            }
        }
        // viewer.playerList is only ever read for index < viewer.playerListSize, and that bound
        // is about to become plan.nextCount below - every slot up to nextCount is rewritten in
        // the loop right after, so a full-array clear here is unnecessary.
        viewer.playersUpdating.clear()
        for (index in 0 until plan.nextCount) {
            val local = plan.next[index] ?: continue
            viewer.playerList[index] = local
            viewer.playersUpdating.add(local)
        }
        viewer.playerListSize = plan.nextCount
        if (changed) viewer.bumpLocalPlayerMembershipRevision()

        // Appearance-ticket bookkeeping, applied only on delivery acceptance (same staging
        // contract as the local list itself):
        //  - additions either carried the full appearance block or their ticket already matched,
        //    so stamping is correct in both cases;
        //  - retained locals delivered appearance this packet only when the APPEARANCE flag was
        //    set (the UPDATE_LOCAL block includes it then). A silent defensive-signature change
        //    without the flag must NOT be stamped — those viewers haven't seen the new look.
        for (index in 0 until plan.additionCount) {
            val addition = plan.additions[index] ?: continue
            viewer.noteAppearanceSeen(addition)
        }
        for (index in 0 until plan.nextCount) {
            val local = plan.next[index] ?: continue
            if (local.updateFlags.isRequired(UpdateFlag.APPEARANCE)) {
                viewer.noteAppearanceSeen(local)
            }
        }
    }

    private fun isRetained(viewer: Client, local: Player?): Boolean {
        local ?: return false
        if (local.slot !in PlayerRegistry.players.indices || PlayerRegistry.players[local.slot] !== local) return false
        return !local.didTeleport() && PlayerVisibilityRules.isVisibleTo(viewer, local)
    }

    class Result internal constructor() {
        var accepted: Boolean = false
            private set
        var packetBytes: Int = 0
            private set
        var previousCount: Int = 0
            private set
        var committedCount: Int = 0
            private set
        var additions: Int = 0
            private set

        internal fun set(accepted: Boolean, packetBytes: Int, previousCount: Int, committedCount: Int, additions: Int): Result {
            this.accepted = accepted
            this.packetBytes = packetBytes
            this.previousCount = previousCount
            this.committedCount = committedCount
            this.additions = additions
            return this
        }
    }

    internal class Plan(capacity: Int) {
        val previous = arrayOfNulls<Player>(capacity)
        val retainPrevious = BooleanArray(capacity)
        val additions = arrayOfNulls<Player>(capacity)
        val next = arrayOfNulls<Player>(capacity)
        val nextGenerations = LongArray(capacity)
        val updateBlocks = arrayOfNulls<PackedUpdateBlock>(capacity + 1)
        var previousCount = 0
        var additionCount = 0
        var nextCount = 0
        var updateBlockCount = 0
            private set
        private var frozen = false

        // previous/retainPrevious/additions/next are never read past their respective counts
        // (previousCount/additionCount/nextCount), and every index up to those counts is
        // rewritten before being read on each buildPlan() call - so clearing the backing
        // arrays here would be pure waste (they were sized for MAX_LOCALS regardless of how
        // many locals a viewer actually has).
        fun reset() {
            previousCount = 0
            additionCount = 0
            nextCount = 0
            resetUpdateBlocks()
            frozen = false
        }

        fun addNext(player: Player) {
            check(!frozen)
            next[nextCount] = player
            nextGenerations[nextCount] = player.synchronizationSessionGeneration
            nextCount++
        }

        fun addAddition(player: Player) {
            check(!frozen)
            additions[additionCount++] = player
            addNext(player)
        }

        fun freeze() { frozen = true }
        fun requireFrozen() = check(frozen) { "Player synchronization plan is not frozen" }

        fun resetUpdateBlocks() {
            Arrays.fill(updateBlocks, 0, updateBlockCount, null)
            updateBlockCount = 0
        }

        fun queueUpdate(block: PackedUpdateBlock) {
            check(updateBlockCount < updateBlocks.size) { "Too many player update blocks" }
            updateBlocks[updateBlockCount++] = block
        }
    }

    private fun canFastSkip(viewer: Client): Boolean {
        if (!viewer.loaded || viewer.didTeleport() || viewer.didMapRegionChange()) return false
        if (viewer.primaryDirection != -1 || viewer.secondaryDirection != -1) return false
        if (updating.hasSelfUpdate(viewer)) return false

        val localCount = viewer.playerListSize
        for (i in 0 until localCount) {
            val local = viewer.playerList[i]
            // Delegate to the same retention predicate buildPlan() uses (slot identity,
            // didTeleport, isVisibleTo/withinDistance/invis) instead of re-checking a subset of
            // it here. The old hand-rolled check only tested isSynchronizationReady+withinDistance
            // and missed didTeleport and invis - a local that teleported (317 has no "moved"
            // encoding for other players, only remove-then-re-add) or went ::invis stayed frozen
            // in the idle fast path indefinitely, since nothing here ever caught the change.
            if (!isRetained(viewer, local)) return false
            if (local!!.primaryDirection != -1 || local.secondaryDirection != -1) return false
            if (local.updateFlags.isUpdateRequired) return false
        }

        return !hasUnadmittedVisibleCandidate(viewer)
    }

    // canFastSkip only re-checks players already in the viewer's local list - without this, a
    // viewer with no pending self-update and no misbehaving existing locals (the common "standing
    // still" case) would take the idle fast path forever and never admit a player who newly walked
    // or logged into view. Mirrors buildPlan's own candidate discovery (same source, same
    // visibility rule) but stops at the first hit instead of building/sorting/budgeting the list.
    private fun hasUnadmittedVisibleCandidate(viewer: Client): Boolean {
        val candidates = SynchronizationContext.getViewportSnapshot(viewer)?.players
        if (candidates != null) {
            candidates.forEach { candidate -> if (isUnadmittedVisible(viewer, candidate)) return true }
        } else {
            PlayerRegistry.players.forEach { candidate -> if (isUnadmittedVisible(viewer, candidate)) return true }
        }
        return false
    }

    private fun isUnadmittedVisible(viewer: Client, candidate: Player?): Boolean {
        candidate ?: return false
        if (viewer.playersUpdating.contains(candidate)) return false
        return PlayerVisibilityRules.isVisibleTo(viewer, candidate)
    }

    private fun encodeIdleFastPath(viewer: Client, localCount: Int, stream: ByteMessage) {
        updating.updateLocalPlayerMovement(viewer, stream, false)
        stream.putBits(8, localCount)
        for (i in 0 until localCount) {
            val local = viewer.playerList[i] ?: continue
            val movement = SynchronizationContext.getSharedPlayerMovement(local)
            if (movement != null) {
                stream.putBitSlice(movement.bytes, 0, movement.bitCount)
            } else {
                stream.putBits(1, 1) // retained
                stream.putBits(2, 0) // no movement, no block
            }
        }
        stream.putBits(11, PlayerUpdating.LOCAL_LIST_TERMINATOR)
        stream.endBitAccess()
    }

    internal class Scratch {
        val plan = Plan(MAX_PLAN_CAPACITY)
        val result = Result()
        val candidates = arrayOfNulls<Player>(MAX_PLAN_CAPACITY)
        private val candidateDistances = IntArray(MAX_PLAN_CAPACITY)
        private val membership = SlotBitSet(gameMaxPlayers + 1)
        var candidateCount = 0
            private set

        fun reset() {
            plan.reset()
            // candidates is only ever read for index < candidateCount (see buildPlan), and
            // that range is fully rewritten by offerCandidate before being read - no fill needed.
            candidateCount = 0
            membership.clear()
        }

        fun offerCandidate(viewer: Client, candidate: Player?, plan: Plan) {
            candidate ?: return
            if (!PlayerVisibilityRules.isVisibleTo(viewer, candidate)) return
            if (membership.contains(candidate.slot)) return
            markMember(candidate.slot)
            val distance = maxOf(
                kotlin.math.abs(viewer.position.x - candidate.position.x),
                kotlin.math.abs(viewer.position.y - candidate.position.y),
            )
            var insert = candidateCount.coerceAtMost(candidates.lastIndex)
            if (candidateCount < candidates.size) candidateCount++
            while (insert > 0) {
                val prior = candidates[insert - 1] ?: break
                val priorDistance = candidateDistances[insert - 1]
                if (priorDistance < distance || (priorDistance == distance && prior.slot <= candidate.slot)) break
                if (insert < candidates.size) {
                    candidates[insert] = prior
                    candidateDistances[insert] = priorDistance
                }
                insert--
            }
            if (insert < candidates.size) {
                candidates[insert] = candidate
                candidateDistances[insert] = distance
            }
        }

        fun markMember(slot: Int) {
            membership.add(slot)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StagedPlayerSynchronizationService::class.java)
        // Wire-protocol cap on simultaneous local players a viewer can track - fixed, unrelated
        // to server player capacity (matches Player.maxPlayerListSize / MAX_PLAN_CAPACITY below).
        private const val MAX_LOCALS = 255
        private const val MAX_PLAN_CAPACITY = 255
        private const val MAX_VAR_SHORT_PAYLOAD = 65535

        // The game-client incoming read buffer is 40,000 bytes (Buffer.create() -> new
        // byte[40_000]); budget well under that so a full-size packet 81 never risks overflowing
        // it alongside whatever else lands in the same read cycle. The prior 60KB constant
        // exceeded the client's own buffer.
        private const val MAX_STAGED_PAYLOAD_BUDGET = 32 * 1024
        private const val BASE_PACKET_BUDGET = 64
        private const val ADD_LOCAL_BIT_BYTES = 4
        private const val MAX_APPEARANCE_BLOCK_ESTIMATE = 256
    }
}
