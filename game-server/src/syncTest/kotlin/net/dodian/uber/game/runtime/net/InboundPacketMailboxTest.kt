package net.dodian.uber.game.runtime.net

import io.netty.buffer.Unpooled
import net.dodian.uber.game.netty.game.GamePacket
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InboundPacketMailboxTest {

    @Test
    fun `walk packets are coalesced and keep order against fifo packets`() {
        val mailbox = InboundPacketMailbox(8)
        val walkA = packet(248, 1)
        val button = packet(185, 2)
        val walkB = packet(164, 3)

        assertTrue(mailbox.enqueue(walkA).accepted())
        assertTrue(mailbox.enqueue(button).accepted())
        assertTrue(mailbox.enqueue(walkB).accepted())

        assertEquals(0, walkA.payload.refCnt(), "superseded walk packet should be released")

        val first = mailbox.pollNext()
        val second = mailbox.pollNext()

        assertEquals(185, first!!.packet().opcode)
        assertEquals(InboundPacketMailbox.Family.FIFO, first.family())
        assertEquals(164, second!!.packet().opcode)
        assertEquals(InboundPacketMailbox.Family.WALK, second.family())
        assertNull(mailbox.pollNext())

        val counters = mailbox.snapshotAndResetCounters()
        assertEquals(1, counters.walkReplaced())
        assertEquals(0, counters.mouseReplaced())
        assertEquals(0, counters.fifoDropped())

        release(first.packet())
        release(second.packet())
    }

    @Test
    fun `fifo overflow drops only fifo packets`() {
        val mailbox = InboundPacketMailbox(1)
        val firstFifo = packet(185, 1)
        val secondFifo = packet(186, 2)
        val walk = packet(98, 3)

        assertTrue(mailbox.enqueue(firstFifo).accepted())
        assertFalse(mailbox.enqueue(secondFifo).accepted())
        assertTrue(mailbox.enqueue(walk).accepted())

        val counters = mailbox.snapshotAndResetCounters()
        assertEquals(1, counters.fifoDropped())
        assertEquals(0, counters.walkReplaced())

        release(secondFifo)
        release(mailbox.pollNext()!!.packet())
        release(mailbox.pollNext()!!.packet())
    }

    private fun packet(opcode: Int, value: Int): GamePacket {
        val payload = Unpooled.buffer(1)
        payload.writeByte(value)
        return GamePacket(opcode, 1, payload)
    }

    private fun release(packet: GamePacket) {
        if (packet.payload.refCnt() > 0) {
            packet.payload.release(packet.payload.refCnt())
        }
    }
}
