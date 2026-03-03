package net.dodian.uber.game.runtime.metrics;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;

/**
 * Minimal, allocation-light inbound opcode profiler.
 *
 * Records the top 3 slowest packet handlers for the current inbound phase and
 * only emits logs when the overall INBOUND_PACKETS phase is slow.
 */
public final class InboundOpcodeProfiler {
    private static final int MAX_SAMPLES_PER_TICK = 256;

    private static int recorded;
    private static Entry top1;
    private static Entry top2;
    private static Entry top3;

    private InboundOpcodeProfiler() {
    }

    public static void beginTick() {
        recorded = 0;
        top1 = null;
        top2 = null;
        top3 = null;
    }

    public static boolean shouldSample() {
        return recorded < MAX_SAMPLES_PER_TICK;
    }

    public static void record(Client player, GamePacket packet, PacketListener listener, long nanos) {
        recorded++;
        if (packet == null || listener == null) {
            return;
        }

        Entry entry = new Entry(
                nanos,
                packet.getOpcode(),
                packet.getSize(),
                listener.getClass().getSimpleName(),
                player == null ? "?" : player.getPlayerName()
        );

        if (top1 == null || entry.nanos > top1.nanos) {
            top3 = top2;
            top2 = top1;
            top1 = entry;
        } else if (top2 == null || entry.nanos > top2.nanos) {
            top3 = top2;
            top2 = entry;
        } else if (top3 == null || entry.nanos > top3.nanos) {
            top3 = entry;
        }
    }

    public static String top3Summary() {
        if (top1 == null) {
            return "n/a";
        }
        return "[" + format(top1) + ", " + format(top2) + ", " + format(top3) + "]";
    }

    private static String format(Entry entry) {
        if (entry == null) {
            return "n/a";
        }
        long ms = entry.nanos / 1_000_000L;
        return entry.listener + "(op=" + entry.opcode + " size=" + entry.size + " player=" + entry.player + " " + ms + "ms)";
    }

    private static final class Entry {
        final long nanos;
        final int opcode;
        final int size;
        final String listener;
        final String player;

        Entry(long nanos, int opcode, int size, String listener, String player) {
            this.nanos = nanos;
            this.opcode = opcode;
            this.size = size;
            this.listener = listener;
            this.player = player;
        }
    }
}

