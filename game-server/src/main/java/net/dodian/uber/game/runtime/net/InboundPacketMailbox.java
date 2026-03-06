package net.dodian.uber.game.runtime.net;

import java.util.ArrayDeque;
import net.dodian.uber.game.netty.game.GamePacket;

/**
 * Per-client inbound mailbox that preserves ordering for transactional packets
 * while collapsing superseding input families.
 */
public final class InboundPacketMailbox {

    public enum Family {
        FIFO,
        WALK,
        MOUSE
    }

    public static final class EnqueueResult {
        private final boolean accepted;
        private final Family family;

        private EnqueueResult(boolean accepted, Family family) {
            this.accepted = accepted;
            this.family = family;
        }

        public boolean accepted() {
            return accepted;
        }

        public Family family() {
            return family;
        }
    }

    public static final class MailboxCounters {
        private final int walkReplaced;
        private final int mouseReplaced;
        private final int fifoDropped;

        private MailboxCounters(int walkReplaced, int mouseReplaced, int fifoDropped) {
            this.walkReplaced = walkReplaced;
            this.mouseReplaced = mouseReplaced;
            this.fifoDropped = fifoDropped;
        }

        public int walkReplaced() {
            return walkReplaced;
        }

        public int mouseReplaced() {
            return mouseReplaced;
        }

        public int fifoDropped() {
            return fifoDropped;
        }
    }

    public static final class PollResult {
        private final GamePacket packet;
        private final Family family;

        private PollResult(GamePacket packet, Family family) {
            this.packet = packet;
            this.family = family;
        }

        public GamePacket packet() {
            return packet;
        }

        public Family family() {
            return family;
        }
    }

    private final int maxPendingPackets;
    private final ArrayDeque<SequencedPacket> transactionalPackets = new ArrayDeque<>();

    private long nextSequence = 0L;
    private int pendingCount = 0;

    private SequencedPacket walkPacket = null;
    private SequencedPacket mousePacket = null;

    private int walkReplacedSinceSnapshot = 0;
    private int mouseReplacedSinceSnapshot = 0;
    private int fifoDroppedSinceSnapshot = 0;

    public InboundPacketMailbox(int maxPendingPackets) {
        this.maxPendingPackets = Math.max(1, maxPendingPackets);
    }

    public synchronized EnqueueResult enqueue(GamePacket packet) {
        if (packet == null) {
            return new EnqueueResult(false, Family.FIFO);
        }
        Family family = familyOf(packet.getOpcode());
        SequencedPacket sequenced = new SequencedPacket(++nextSequence, family, packet);
        switch (family) {
            case WALK:
                replaceSupersedingPacket(sequenced, true);
                return new EnqueueResult(true, family);
            case MOUSE:
                replaceSupersedingPacket(sequenced, false);
                return new EnqueueResult(true, family);
            case FIFO:
            default:
                if (pendingCount >= maxPendingPackets) {
                    fifoDroppedSinceSnapshot++;
                    return new EnqueueResult(false, family);
                }
                transactionalPackets.addLast(sequenced);
                pendingCount++;
                return new EnqueueResult(true, family);
        }
    }

    public synchronized PollResult pollNext() {
        SequencedPacket transactional = transactionalPackets.peekFirst();
        SequencedPacket candidate = transactional;
        if (walkPacket != null && (candidate == null || walkPacket.sequence < candidate.sequence)) {
            candidate = walkPacket;
        }
        if (mousePacket != null && (candidate == null || mousePacket.sequence < candidate.sequence)) {
            candidate = mousePacket;
        }
        if (candidate == null) {
            return null;
        }

        if (candidate == transactional) {
            transactionalPackets.removeFirst();
        } else if (candidate == walkPacket) {
            walkPacket = null;
        } else if (candidate == mousePacket) {
            mousePacket = null;
        }
        pendingCount--;
        return new PollResult(candidate.packet, candidate.family);
    }

    public synchronized int pendingCount() {
        return pendingCount;
    }

    public synchronized MailboxCounters snapshotAndResetCounters() {
        MailboxCounters counters =
            new MailboxCounters(
                walkReplacedSinceSnapshot,
                mouseReplacedSinceSnapshot,
                fifoDroppedSinceSnapshot
            );
        walkReplacedSinceSnapshot = 0;
        mouseReplacedSinceSnapshot = 0;
        fifoDroppedSinceSnapshot = 0;
        return counters;
    }

    public synchronized void clear(PacketReleaser releaser) {
        while (!transactionalPackets.isEmpty()) {
            releaser.release(transactionalPackets.removeFirst().packet);
        }
        if (walkPacket != null) {
            releaser.release(walkPacket.packet);
            walkPacket = null;
        }
        if (mousePacket != null) {
            releaser.release(mousePacket.packet);
            mousePacket = null;
        }
        pendingCount = 0;
    }

    private void replaceSupersedingPacket(SequencedPacket packet, boolean walkFamily) {
        SequencedPacket previous = walkFamily ? walkPacket : mousePacket;
        if (previous == null) {
            pendingCount++;
        } else if (walkFamily) {
            walkReplacedSinceSnapshot++;
            release(previous.packet);
        } else {
            mouseReplacedSinceSnapshot++;
            release(previous.packet);
        }
        if (walkFamily) {
            walkPacket = packet;
        } else {
            mousePacket = packet;
        }
    }

    private void release(GamePacket packet) {
        if (packet != null && packet.getPayload() != null && packet.getPayload().refCnt() > 0) {
            packet.getPayload().release();
        }
    }

    private static Family familyOf(int opcode) {
        if (opcode == 248 || opcode == 164 || opcode == 98) {
            return Family.WALK;
        }
        if (opcode == 241) {
            return Family.MOUSE;
        }
        return Family.FIFO;
    }

    @FunctionalInterface
    public interface PacketReleaser {
        void release(GamePacket packet);
    }

    private static final class SequencedPacket {
        private final long sequence;
        private final Family family;
        private final GamePacket packet;

        private SequencedPacket(long sequence, Family family, GamePacket packet) {
            this.sequence = sequence;
            this.family = family;
            this.packet = packet;
        }
    }
}
