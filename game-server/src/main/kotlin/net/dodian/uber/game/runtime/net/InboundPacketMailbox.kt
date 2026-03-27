package net.dodian.uber.game.runtime.net

import java.util.ArrayDeque
import net.dodian.uber.game.netty.game.GamePacket

/**
 * Per-client inbound mailbox that preserves ordering for transactional packets
 * while collapsing superseding input families.
 */
class InboundPacketMailbox(maxPendingPackets: Int) {
    enum class Family {
        FIFO,
        WALK,
        MOUSE,
    }

    class EnqueueResult private constructor(
        private val accepted: Boolean,
        private val family: Family,
    ) {
        fun accepted(): Boolean = accepted

        fun family(): Family = family

        companion object {
            @JvmStatic
            fun of(accepted: Boolean, family: Family): EnqueueResult = EnqueueResult(accepted, family)
        }
    }

    class MailboxCounters private constructor(
        private val walkReplaced: Int,
        private val mouseReplaced: Int,
        private val fifoDropped: Int,
    ) {
        fun walkReplaced(): Int = walkReplaced

        fun mouseReplaced(): Int = mouseReplaced

        fun fifoDropped(): Int = fifoDropped

        companion object {
            private val EMPTY = MailboxCounters(0, 0, 0)

            @JvmStatic
            fun empty(): MailboxCounters = EMPTY

            @JvmStatic
            fun of(walkReplaced: Int, mouseReplaced: Int, fifoDropped: Int): MailboxCounters =
                if (walkReplaced == 0 && mouseReplaced == 0 && fifoDropped == 0) {
                    EMPTY
                } else {
                    MailboxCounters(walkReplaced, mouseReplaced, fifoDropped)
                }
        }
    }

    class PollResult private constructor(
        private val packet: GamePacket,
        private val family: Family,
    ) {
        fun packet(): GamePacket = packet

        fun family(): Family = family

        companion object {
            @JvmStatic
            fun of(packet: GamePacket, family: Family): PollResult = PollResult(packet, family)
        }
    }

    fun interface PacketReleaser {
        fun release(packet: GamePacket?)
    }

    private class SequencedPacket(
        val sequence: Long,
        val family: Family,
        val packet: GamePacket,
    )

    private val maxPendingPackets = maxOf(1, maxPendingPackets)
    private val transactionalPackets = ArrayDeque<SequencedPacket>()

    private var nextSequence = 0L
    private var pendingCount = 0

    private var walkPacket: SequencedPacket? = null
    private var mousePacket: SequencedPacket? = null

    private var walkReplacedSinceSnapshot = 0
    private var mouseReplacedSinceSnapshot = 0
    private var fifoDroppedSinceSnapshot = 0

    @Synchronized
    fun enqueue(packet: GamePacket?): EnqueueResult {
        if (packet == null) {
            return EnqueueResult.of(false, Family.FIFO)
        }
        val family = familyOf(packet.opcode())
        val sequenced = SequencedPacket(++nextSequence, family, packet)
        return when (family) {
            Family.WALK -> {
                replaceSupersedingPacket(sequenced, walkFamily = true)
                EnqueueResult.of(true, family)
            }
            Family.MOUSE -> {
                replaceSupersedingPacket(sequenced, walkFamily = false)
                EnqueueResult.of(true, family)
            }
            Family.FIFO -> {
                if (pendingCount >= maxPendingPackets) {
                    fifoDroppedSinceSnapshot++
                    EnqueueResult.of(false, family)
                } else {
                    transactionalPackets.addLast(sequenced)
                    pendingCount++
                    EnqueueResult.of(true, family)
                }
            }
        }
    }

    @Synchronized
    fun pollNext(): PollResult? {
        val transactional = transactionalPackets.peekFirst()
        var candidate = transactional
        val currentWalk = walkPacket
        if (currentWalk != null && (candidate == null || currentWalk.sequence < candidate.sequence)) {
            candidate = currentWalk
        }
        val currentMouse = mousePacket
        if (currentMouse != null && (candidate == null || currentMouse.sequence < candidate.sequence)) {
            candidate = currentMouse
        }
        candidate ?: return null

        when (candidate) {
            transactional -> transactionalPackets.removeFirst()
            currentWalk -> walkPacket = null
            currentMouse -> mousePacket = null
        }
        pendingCount--
        return PollResult.of(candidate.packet, candidate.family)
    }

    @Synchronized
    fun pendingCount(): Int = pendingCount

    @Synchronized
    fun snapshotAndResetCounters(): MailboxCounters {
        val counters = MailboxCounters.of(walkReplacedSinceSnapshot, mouseReplacedSinceSnapshot, fifoDroppedSinceSnapshot)
        walkReplacedSinceSnapshot = 0
        mouseReplacedSinceSnapshot = 0
        fifoDroppedSinceSnapshot = 0
        return counters
    }

    @Synchronized
    fun clear(releaser: PacketReleaser) {
        while (!transactionalPackets.isEmpty()) {
            releaser.release(transactionalPackets.removeFirst().packet)
        }
        val currentWalk = walkPacket
        if (currentWalk != null) {
            releaser.release(currentWalk.packet)
            walkPacket = null
        }
        val currentMouse = mousePacket
        if (currentMouse != null) {
            releaser.release(currentMouse.packet)
            mousePacket = null
        }
        pendingCount = 0
    }

    private fun replaceSupersedingPacket(packet: SequencedPacket, walkFamily: Boolean) {
        val previous = if (walkFamily) walkPacket else mousePacket
        if (previous == null) {
            pendingCount++
        } else if (walkFamily) {
            walkReplacedSinceSnapshot++
            release(previous.packet)
        } else {
            mouseReplacedSinceSnapshot++
            release(previous.packet)
        }
        if (walkFamily) {
            walkPacket = packet
        } else {
            mousePacket = packet
        }
    }

    private fun release(packet: GamePacket?) {
        val payload = packet?.payload() ?: return
        if (payload.refCnt() > 0) {
            payload.release()
        }
    }

    companion object {
        @JvmStatic
        fun familyOf(opcode: Int): Family =
            when (opcode) {
                248, 164, 98 -> Family.WALK
                241 -> Family.MOUSE
                else -> Family.FIFO
            }
    }
}
