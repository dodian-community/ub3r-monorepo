package net.dodian.uber.game.engine.net

import io.netty.buffer.Unpooled
import net.dodian.uber.game.netty.game.GamePacket
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InboundPacketMailboxTest {
    private fun packet(opcode: Int) = GamePacket(opcode, 0, Unpooled.EMPTY_BUFFER)

    @Test
    fun `action opcodes drain before background opcodes even when background was enqueued first`() {
        val mailbox = InboundPacketMailbox(200)
        // Bank preset action.
        val actionOpcode = 43
        // Public chat.
        val backgroundOpcode = 4

        assertTrue(mailbox.enqueue(packet(backgroundOpcode)).accepted())
        assertTrue(mailbox.enqueue(packet(actionOpcode)).accepted())

        val first = mailbox.pollNext()
        assertEquals(InboundPacketMailbox.Family.ACTION, first?.family())
        assertEquals(actionOpcode, first?.packet()?.opcode())

        val second = mailbox.pollNext()
        assertEquals(InboundPacketMailbox.Family.BACKGROUND, second?.family())
        assertEquals(backgroundOpcode, second?.packet()?.opcode())
    }

    @Test
    fun `flooding the background lane cannot consume action lane capacity`() {
        val mailbox = InboundPacketMailbox(200)
        // Bank-search-as-you-type keystrokes (background) flooding far past its own budget.
        repeat(1000) { mailbox.enqueue(packet(142)) }

        // Action packets must still be accepted at full capacity afterward.
        var accepted = 0
        repeat(200) {
            if (mailbox.enqueue(packet(43)).accepted()) accepted++
        }
        assertEquals(200, accepted, "background flood must not shrink the action lane's budget")
    }

    @Test
    fun `background lane has its own independent, smaller capacity`() {
        val mailbox = InboundPacketMailbox(200)
        var accepted = 0
        var rejected = 0
        repeat(200) {
            if (mailbox.enqueue(packet(4)).accepted()) accepted++ else rejected++
        }
        assertTrue(accepted < 200, "background lane must be capped below the main capacity")
        assertTrue(rejected > 0)
    }

    @Test
    fun `walk mouse and item-click coalescing is unaffected by the action-background split`() {
        val mailbox = InboundPacketMailbox(200)
        assertTrue(mailbox.enqueue(packet(248)).accepted())
        assertTrue(mailbox.enqueue(packet(248)).accepted())
        val counters = mailbox.snapshotAndResetCounters()
        assertEquals(1, counters.walkReplaced())
        assertEquals(1, mailbox.pendingCount())

        val result = mailbox.pollNext()
        assertEquals(InboundPacketMailbox.Family.WALK, result?.family())
        assertEquals(0, mailbox.pendingCount())
        assertFalse(mailbox.enqueue(null).accepted())
    }

    @Test
    fun `server-owned action can discard an older predictive walk`() {
        val mailbox = InboundPacketMailbox(200)
        assertTrue(mailbox.enqueue(packet(248)).accepted())
        assertTrue(mailbox.enqueue(packet(39)).accepted())

        val action = mailbox.pollNext()
        assertEquals(InboundPacketMailbox.Family.ACTION, action?.family())
        assertTrue(mailbox.discardWalkAtOrBefore(requireNotNull(action).sequence()))
        assertEquals(0, mailbox.pendingCount())
    }

    @Test
    fun `server-owned action preserves newer deliberate walking`() {
        val mailbox = InboundPacketMailbox(200)
        assertTrue(mailbox.enqueue(packet(39)).accepted())
        assertTrue(mailbox.enqueue(packet(248)).accepted())

        val action = mailbox.pollNext()
        assertEquals(InboundPacketMailbox.Family.ACTION, action?.family())
        assertFalse(mailbox.discardWalkAtOrBefore(requireNotNull(action).sequence()))

        val walk = mailbox.pollNext()
        assertEquals(InboundPacketMailbox.Family.WALK, walk?.family())
        assertTrue(requireNotNull(walk).sequence() > action.sequence())
    }
}
