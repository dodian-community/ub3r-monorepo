package net.dodian.uber.game.engine.net

import java.util.ArrayDeque
import net.dodian.uber.game.netty.game.GamePacket

/**
 * Per-client inbound mailbox that preserves ordering for transactional packets
 * while collapsing superseding input families.
 *
 * The FIFO bucket is itself split into two independently-budgeted lanes (rsprot's
 * client-event/user-event split, adapted to this protocol): [Family.ACTION] for
 * packets that represent a direct, single player-intended game action (bank ops,
 * object/npc/item interactions, buttons, trade/duel requests) and [Family.BACKGROUND]
 * for lower-urgency chatter (chat, friends/ignore list edits, bank-search keystrokes,
 * dropdowns, focus changes). Each lane has its own capacity so a flood of one can
 * never crowd out the other's slots, and [pollNext] always drains ACTION before
 * BACKGROUND so gameplay-critical input isn't stuck behind e.g. a burst of bank
 * search keystrokes.
 */
class InboundPacketMailbox(maxPendingPackets: Int) {
    enum class Family {
        ACTION,
        BACKGROUND,
        WALK,
        MOUSE,
        ITEM_CLICK,
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
        private val itemClickReplaced: Int,
        private val fifoDropped: Int,
    ) {
        fun walkReplaced(): Int = walkReplaced

        fun mouseReplaced(): Int = mouseReplaced

        fun itemClickReplaced(): Int = itemClickReplaced

        /** Total drops across both the ACTION and BACKGROUND lanes. */
        fun fifoDropped(): Int = fifoDropped

        companion object {
            private val EMPTY = MailboxCounters(0, 0, 0, 0)

            @JvmStatic
            fun empty(): MailboxCounters = EMPTY

            @JvmStatic
            fun of(walkReplaced: Int, mouseReplaced: Int, itemClickReplaced: Int, fifoDropped: Int): MailboxCounters =
                if (walkReplaced == 0 && mouseReplaced == 0 && itemClickReplaced == 0 && fifoDropped == 0) {
                    EMPTY
                } else {
                    MailboxCounters(walkReplaced, mouseReplaced, itemClickReplaced, fifoDropped)
                }
        }
    }

    class PollResult private constructor(
        private val packet: GamePacket,
        private val family: Family,
        private val sequence: Long,
    ) {
        fun packet(): GamePacket = packet

        fun family(): Family = family

        fun sequence(): Long = sequence

        companion object {
            @JvmStatic
            fun of(packet: GamePacket, family: Family, sequence: Long): PollResult =
                PollResult(packet, family, sequence)
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

    private val actionCapacity = maxOf(1, maxPendingPackets)

    // The background lane deliberately gets a smaller, separate budget rather than a
    // share of actionCapacity: it must never be able to consume slots that would
    // otherwise be available to gameplay-critical ACTION packets.
    private val backgroundCapacity = maxOf(BACKGROUND_CAPACITY_FLOOR, actionCapacity / BACKGROUND_CAPACITY_DIVISOR)

    private val actionPackets = ArrayDeque<SequencedPacket>()
    private val backgroundPackets = ArrayDeque<SequencedPacket>()

    private var nextSequence = 0L
    private var pendingCount = 0

    private var walkPacket: SequencedPacket? = null
    private var mousePacket: SequencedPacket? = null
    private var itemClickPacket: SequencedPacket? = null

    private var walkReplacedSinceSnapshot = 0
    private var mouseReplacedSinceSnapshot = 0
    private var itemClickReplacedSinceSnapshot = 0
    private var actionDroppedSinceSnapshot = 0
    private var backgroundDroppedSinceSnapshot = 0

    @Synchronized
    fun enqueue(packet: GamePacket?): EnqueueResult {
        if (packet == null) {
            return EnqueueResult.of(false, Family.ACTION)
        }
        val family = familyOf(packet.opcode())
        val sequenced = SequencedPacket(++nextSequence, family, packet)
        return when (family) {
            Family.WALK, Family.MOUSE, Family.ITEM_CLICK -> {
                replaceSupersedingPacket(sequenced, family)
                EnqueueResult.of(true, family)
            }
            Family.ACTION -> {
                if (actionPackets.size >= actionCapacity) {
                    actionDroppedSinceSnapshot++
                    EnqueueResult.of(false, family)
                } else {
                    actionPackets.addLast(sequenced)
                    pendingCount++
                    EnqueueResult.of(true, family)
                }
            }
            Family.BACKGROUND -> {
                if (backgroundPackets.size >= backgroundCapacity) {
                    backgroundDroppedSinceSnapshot++
                    EnqueueResult.of(false, family)
                } else {
                    backgroundPackets.addLast(sequenced)
                    pendingCount++
                    EnqueueResult.of(true, family)
                }
            }
        }
    }

    @Synchronized
    fun pollNext(): PollResult? {
        val action = actionPackets.pollFirst()
        if (action != null) {
            pendingCount--
            return PollResult.of(action.packet, action.family, action.sequence)
        }

        val currentWalk = walkPacket
        if (currentWalk != null) {
            walkPacket = null
            pendingCount--
            return PollResult.of(currentWalk.packet, currentWalk.family, currentWalk.sequence)
        }

        val currentItemClick = itemClickPacket
        if (currentItemClick != null) {
            itemClickPacket = null
            pendingCount--
            return PollResult.of(currentItemClick.packet, currentItemClick.family, currentItemClick.sequence)
        }

        val currentMouse = mousePacket
        if (currentMouse != null) {
            mousePacket = null
            pendingCount--
            return PollResult.of(currentMouse.packet, currentMouse.family, currentMouse.sequence)
        }

        val background = backgroundPackets.pollFirst()
        if (background != null) {
            pendingCount--
            return PollResult.of(background.packet, background.family, background.sequence)
        }

        return null
    }

    @Synchronized
    fun pendingCount(): Int = pendingCount

    /**
     * Drops client-predicted walking that preceded a server-owned interaction.
     *
     * A newer walk is deliberate follow-up input and must remain queued.
     */
    @Synchronized
    fun discardWalkAtOrBefore(sequence: Long): Boolean {
        val currentWalk = walkPacket ?: return false
        if (currentWalk.sequence > sequence) return false
        walkPacket = null
        pendingCount--
        release(currentWalk.packet)
        return true
    }

    @Synchronized
    fun snapshotAndResetCounters(): MailboxCounters {
        val counters = MailboxCounters.of(
            walkReplacedSinceSnapshot,
            mouseReplacedSinceSnapshot,
            itemClickReplacedSinceSnapshot,
            actionDroppedSinceSnapshot + backgroundDroppedSinceSnapshot,
        )
        walkReplacedSinceSnapshot = 0
        mouseReplacedSinceSnapshot = 0
        itemClickReplacedSinceSnapshot = 0
        actionDroppedSinceSnapshot = 0
        backgroundDroppedSinceSnapshot = 0
        return counters
    }

    @Synchronized
    fun clear(releaser: PacketReleaser) {
        while (!actionPackets.isEmpty()) {
            releaser.release(actionPackets.removeFirst().packet)
        }
        while (!backgroundPackets.isEmpty()) {
            releaser.release(backgroundPackets.removeFirst().packet)
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
        val currentItemClick = itemClickPacket
        if (currentItemClick != null) {
            releaser.release(currentItemClick.packet)
            itemClickPacket = null
        }
        pendingCount = 0
    }

    private fun replaceSupersedingPacket(packet: SequencedPacket, family: Family) {
        val previous = when (family) {
            Family.WALK -> walkPacket
            Family.MOUSE -> mousePacket
            Family.ITEM_CLICK -> itemClickPacket
            Family.ACTION, Family.BACKGROUND -> null
        }
        if (previous == null) {
            pendingCount++
        } else {
            when (family) {
                Family.WALK -> walkReplacedSinceSnapshot++
                Family.MOUSE -> mouseReplacedSinceSnapshot++
                Family.ITEM_CLICK -> itemClickReplacedSinceSnapshot++
                Family.ACTION, Family.BACKGROUND -> Unit
            }
            release(previous.packet)
        }
        when (family) {
            Family.WALK -> walkPacket = packet
            Family.MOUSE -> mousePacket = packet
            Family.ITEM_CLICK -> itemClickPacket = packet
            Family.ACTION, Family.BACKGROUND -> Unit
        }
    }

    private fun release(packet: GamePacket?) {
        val payload = packet?.payload() ?: return
        if (payload.refCnt() > 0) {
            payload.release()
        }
    }

    companion object {
        /** Opcode 122: first-click item action (e.g. bury bones, eat food, drink potion). */
        private const val OPCODE_ITEM_CLICK = 122

        /** Background lane gets at most 1/4 of the main capacity... */
        private const val BACKGROUND_CAPACITY_DIVISOR = 4

        /** ...but never less than this, so legitimate chat/friends-list bursts still fit. */
        private const val BACKGROUND_CAPACITY_FLOOR = 32

        /**
         * Direct, single player-intended game actions — gameplay depends on these being
         * processed promptly, so they get the full mailbox budget and top drain priority.
         */
        private val ACTION_OPCODES = intArrayOf(
            // Object interactions: click options 1-5, item-on-object, magic-on-object.
            132, 252, 70, 234, 228, 192, 35,
            // NPC interactions: click options + attack + magic-on-npc.
            155, 17, 21, 18, 72, 73, 131,
            // Item interactions: second/third click, item-on-item/npc/player, magic-on-item.
            16, 75, 53, 57, 14, 237, 249,
            // Bank family + move-items + pickup + drop.
            43, 117, 129, 140, 141, 135, 208, 214, 236, 87,
            // Buttons and player-menu (trade/duel/follow) requests.
            185, 139, 128, 39, 153,
            // Dialogue advance — the player is blocked waiting on this to progress.
            40,
        )

        private val ACTION_OPCODE_SET: Set<Int> = ACTION_OPCODES.toHashSet()

        @JvmStatic
        fun familyOf(opcode: Int): Family =
            when (opcode) {
                248, 164, 98 -> Family.WALK
                241 -> Family.MOUSE
                OPCODE_ITEM_CLICK -> Family.ITEM_CLICK
                in ACTION_OPCODE_SET -> Family.ACTION
                else -> Family.BACKGROUND
            }
    }
}
