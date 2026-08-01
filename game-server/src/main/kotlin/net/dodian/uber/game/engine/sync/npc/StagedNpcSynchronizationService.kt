package net.dodian.uber.game.engine.sync.npc

import io.netty.buffer.ByteBuf
import java.util.Arrays
import net.dodian.uber.game.engine.metrics.OperationalTelemetry
import net.dodian.uber.game.engine.sync.SynchronizationContext
import net.dodian.uber.game.engine.sync.scratch.ThreadLocalSyncScratch
import net.dodian.uber.game.engine.sync.protocol.PackedUpdateBlock
import net.dodian.uber.game.engine.sync.util.SlotBitSet
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.npc.NpcUpdating
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.netty.codec.MessageType

/** Stages protocol-compatible NPC local state and commits only after delivery acceptance. */
class StagedNpcSynchronizationService {
    private val updating = NpcUpdating.getInstance()
    private val scratch = ThreadLocal.withInitial(::Plan)
    private val resultScratch = ThreadLocal.withInitial(::Result)

    fun synchronize(viewer: Client): Result {
        val plan = buildPlan(viewer, scratch.get())
        val result = resultScratch.get()
        val capacity = maxOf(1024, viewer.npcUpdateCapacity)
        val pooledBuffer: ByteBuf = viewer.channel?.alloc()?.buffer(capacity) ?: ByteMessage.pooledBuffer(capacity)
        var message: ByteMessage? = null
        try {
            message = ByteMessage.message(65, MessageType.VAR_SHORT, pooledBuffer)
            encode(viewer, plan, message)
            val packetBytes = message.content().writerIndex()
            require(packetBytes <= MAX_VAR_SHORT_PAYLOAD) { "NPC synchronization payload exceeds protocol limit: $packetBytes" }
            viewer.updateNpcUpdateCapacity(packetBytes)
            OperationalTelemetry.incrementCounter("sync.npc.packet_bytes", packetBytes.toLong())
            OperationalTelemetry.incrementCounter("sync.npc.planned_additions", plan.additionCount.toLong())
            val accepted = viewer.sendRequiredSynchronization(message)
            message = null
            if (!accepted) {
                OperationalTelemetry.incrementCounter("sync.npc.required_enqueue_rejected")
                return result.set(false, packetBytes, plan.previousCount, plan.nextCount, plan.additionCount)
            }
            commit(viewer, plan)
            OperationalTelemetry.incrementCounter("sync.npc.staged_committed")
            OperationalTelemetry.incrementCounter("sync.npc.committed_locals", plan.nextCount.toLong())
            return result.set(true, packetBytes, plan.previousCount, plan.nextCount, plan.additionCount)
        } finally {
            message?.releaseAll()
        }
    }

    internal fun buildPlan(viewer: Client, plan: Plan = scratch.get()): Plan {
        plan.reset()
        plan.previousCount = viewer.localNpcSize.coerceIn(0, minOf(MAX_LOCALS, viewer.localNpcs.size))
        for (index in 0 until plan.previousCount) {
            val npc = viewer.localNpcs[index]
            plan.previous[index] = npc
            val retained = isRetained(viewer, npc)
            plan.retainPrevious[index] = retained
            if (retained && npc != null) plan.addNext(npc)
        }

        val nearby = SynchronizationContext.getViewportSnapshot(viewer)?.npcs
            ?: net.dodian.uber.game.Server.npcManager.getNpcs()
        var estimatedBytes = BASE_PACKET_BUDGET
        for (npc in nearby) {
            if (plan.additionCount >= MAX_ADDITIONS_PER_CYCLE || plan.nextCount >= MAX_LOCALS) break
            if (!isRetained(viewer, npc) || plan.contains(npc)) continue
            val sharedBlock = SynchronizationContext.getSharedNpcBlock(npc)
            val additionBytes = ADD_NPC_BIT_BYTES + (sharedBlock?.estimatedWireBytes(false) ?: MAX_NPC_BLOCK_ESTIMATE)
            if (estimatedBytes + additionBytes > MAX_STAGED_NPC_PAYLOAD_BUDGET) break
            estimatedBytes += additionBytes
            plan.addAddition(npc)
        }
        plan.freeze()
        return plan
    }

    internal fun encode(viewer: Client, plan: Plan, stream: ByteMessage) {
        plan.requireFrozen()
        plan.resetUpdateBlocks()
        stream.startBitAccess()
        stream.putBits(8, plan.previousCount)
        for (index in 0 until plan.previousCount) {
            val npc = plan.previous[index]
            if (!plan.retainPrevious[index] || npc == null) {
                updating.writeLocalRemoval(stream)
            } else {
                val movement = SynchronizationContext.getSharedNpcMovement(npc)
                val block = if (npc.updateFlags.isUpdateRequired) sharedOrEncode(npc) else null
                if (movement != null) {
                    stream.putBitSlice(movement.bytes, 0, movement.bitCount)
                } else {
                    updating.updateNPCMovement(npc, stream, block != null)
                }
                if (block != null) {
                    plan.queueUpdate(block)
                }
            }
        }
        for (index in 0 until plan.additionCount) {
            val npc = plan.additions[index] ?: continue
            val block = if (npc.updateFlags.isUpdateRequired) sharedOrEncode(npc) else null
            updating.addNpc(viewer, npc, stream, block != null)
            if (block != null) {
                plan.queueUpdate(block)
            }
            SynchronizationContext.recordNpcAdd()
        }
        if (plan.updateBlockCount > 0) {
            stream.putBits(NpcUpdating.NPC_SLOT_BITS, NpcUpdating.NPC_SLOT_TERMINATOR)
            stream.endBitAccess()
            writePackedUpdatePlanes(plan, stream)
        } else {
            stream.endBitAccess()
        }
    }

    private fun sharedOrEncode(npc: Npc): PackedUpdateBlock? {
        val sharedBlock = SynchronizationContext.getSharedNpcBlock(npc)
        SynchronizationContext.recordNpcBlockCacheHit(sharedBlock != null)
        return sharedBlock ?: updating.buildPackedBlock(npc)
    }

    private fun writePackedUpdatePlanes(plan: Plan, stream: ByteMessage) {
        for (index in 0 until plan.updateBlockCount) {
            stream.put(requireNotNull(plan.updateBlocks[index]).mask)
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
        var changed = viewer.localNpcSize != plan.nextCount
        if (!changed) {
            for (index in 0 until plan.nextCount) {
                if (viewer.localNpcs[index] !== plan.next[index]) {
                    changed = true
                    break
                }
            }
        }
        // viewer.localNpcs is only ever read for index < viewer.localNpcSize, and that bound is
        // about to become plan.nextCount below - every slot up to nextCount is rewritten in the
        // loop right after, so a full-array clear here is unnecessary.
        for (index in 0 until plan.nextCount) viewer.localNpcs[index] = plan.next[index]
        viewer.localNpcSize = plan.nextCount
        if (changed) viewer.bumpLocalNpcMembershipRevision()
    }

    private fun isRetained(viewer: Client, npc: Npc?): Boolean =
        npc != null && viewer.withinDistance(npc) && npc.isVisible && !NpcUpdating.removeNpc(viewer, npc)

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

    internal class Plan {
        val previous = arrayOfNulls<Npc>(MAX_LOCALS)
        val retainPrevious = BooleanArray(MAX_LOCALS)
        val additions = arrayOfNulls<Npc>(MAX_LOCALS)
        val next = arrayOfNulls<Npc>(MAX_LOCALS)
        val nextSlots = IntArray(MAX_LOCALS)
        // O(1) membership instead of an O(n) linear scan of nextSlots per candidate per viewer per
        // tick (mirrors the player-side Scratch.membership pattern).
        private val membership = SlotBitSet(1 shl NpcUpdating.NPC_SLOT_BITS)
        val updateBlocks = arrayOfNulls<PackedUpdateBlock>(MAX_LOCALS)
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
            membership.clear()
            previousCount = 0
            additionCount = 0
            nextCount = 0
            resetUpdateBlocks()
            frozen = false
        }

        fun addNext(npc: Npc) {
            check(!frozen)
            next[nextCount] = npc
            nextSlots[nextCount] = npc.slot
            nextCount++
            membership.add(npc.slot)
        }

        fun addAddition(npc: Npc) {
            additions[additionCount++] = npc
            addNext(npc)
        }

        fun contains(npc: Npc): Boolean = membership.contains(npc.slot)

        fun freeze() { frozen = true }
        fun requireFrozen() = check(frozen) { "NPC synchronization plan is not frozen" }

        fun resetUpdateBlocks() {
            Arrays.fill(updateBlocks, 0, updateBlockCount, null)
            updateBlockCount = 0
        }

        fun queueUpdate(block: PackedUpdateBlock) {
            check(updateBlockCount < updateBlocks.size) { "Too many NPC update blocks" }
            updateBlocks[updateBlockCount++] = block
        }
    }

    companion object {
        private const val MAX_LOCALS = 255
        private const val MAX_ADDITIONS_PER_CYCLE = 25
        private const val MAX_VAR_SHORT_PAYLOAD = 65535

        // The game-client incoming read buffer is 40,000 bytes (Buffer.create() -> new
        // byte[40_000]); budget well under that so a full-size packet 65 never risks overflowing
        // it alongside whatever else lands in the same read cycle. Mirrors the player-side budget.
        private const val MAX_STAGED_NPC_PAYLOAD_BUDGET = 32 * 1024
        private const val BASE_PACKET_BUDGET = 64
        private const val ADD_NPC_BIT_BYTES = 6
        private const val MAX_NPC_BLOCK_ESTIMATE = 128
    }
}
