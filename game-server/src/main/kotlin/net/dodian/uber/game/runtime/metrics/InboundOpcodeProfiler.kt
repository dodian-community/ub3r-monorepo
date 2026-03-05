package net.dodian.uber.game.runtime.metrics

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.game.GamePacket
import net.dodian.uber.game.netty.listener.PacketListener

/**
 * Minimal, allocation-light inbound opcode profiler.
 *
 * Records the top 3 slowest packet handlers for the current inbound phase and
 * only emits logs when the overall INBOUND_PACKETS phase is slow.
 */
object InboundOpcodeProfiler {
    private const val MAX_SAMPLES_PER_TICK = 256

    private var recorded = 0
    private var top1: Entry? = null
    private var top2: Entry? = null
    private var top3: Entry? = null

    @JvmStatic
    fun beginTick() {
        recorded = 0
        top1 = null
        top2 = null
        top3 = null
    }

    @JvmStatic
    fun shouldSample(): Boolean = recorded < MAX_SAMPLES_PER_TICK

    @JvmStatic
    fun record(player: Client?, packet: GamePacket?, listener: PacketListener?, nanos: Long) {
        recorded++
        if (packet == null || listener == null) {
            return
        }

        val entry = Entry(
            nanos,
            packet.opcode,
            packet.size,
            listener.javaClass.simpleName,
            player?.playerName ?: "?",
        )

        if (top1 == null || entry.nanos > top1!!.nanos) {
            top3 = top2
            top2 = top1
            top1 = entry
        } else if (top2 == null || entry.nanos > top2!!.nanos) {
            top3 = top2
            top2 = entry
        } else if (top3 == null || entry.nanos > top3!!.nanos) {
            top3 = entry
        }
    }

    @JvmStatic
    fun top3Summary(): String {
        if (top1 == null) {
            return "n/a"
        }
        return "[${format(top1)}, ${format(top2)}, ${format(top3)}]"
    }

    private fun format(entry: Entry?): String {
        if (entry == null) {
            return "n/a"
        }
        val ms = entry.nanos / 1_000_000L
        return "${entry.listener}(op=${entry.opcode} size=${entry.size} player=${entry.player} ${ms}ms)"
    }

    private data class Entry(
        val nanos: Long,
        val opcode: Int,
        val size: Int,
        val listener: String,
        val player: String,
    )
}
