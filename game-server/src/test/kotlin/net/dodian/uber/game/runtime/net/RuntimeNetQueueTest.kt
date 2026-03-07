package net.dodian.uber.game.runtime.net

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.netty.game.GamePacket
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuntimeNetQueueTest {
    @Test
    fun `inbound mailbox preserves fifo ordering for transactional packets`() {
        val mailbox = InboundPacketMailbox(8)
        val first = packet(10)
        val second = packet(11)

        mailbox.enqueue(first)
        mailbox.enqueue(second)

        val polledFirst = mailbox.pollNext()
        val polledSecond = mailbox.pollNext()

        assertSame(first, polledFirst?.packet())
        assertEquals(InboundPacketMailbox.Family.FIFO, polledFirst?.family())
        assertSame(second, polledSecond?.packet())
        assertEquals(InboundPacketMailbox.Family.FIFO, polledSecond?.family())

        release(polledFirst?.packet())
        release(polledSecond?.packet())
    }

    @Test
    fun `inbound mailbox replaces walk packets and releases prior payload`() {
        val mailbox = InboundPacketMailbox(8)
        val firstWalk = packet(248)
        val secondWalk = packet(164)

        mailbox.enqueue(firstWalk)
        mailbox.enqueue(secondWalk)

        assertEquals(0, firstWalk.payload.refCnt())
        val counters = mailbox.snapshotAndResetCounters()
        val polled = mailbox.pollNext()

        assertEquals(1, counters.walkReplaced())
        assertSame(secondWalk, polled?.packet())
        assertEquals(InboundPacketMailbox.Family.WALK, polled?.family())

        release(polled?.packet())
    }

    @Test
    fun `inbound mailbox replaces mouse packets and preserves sequence ordering`() {
        val mailbox = InboundPacketMailbox(8)
        val fifo = packet(20)
        val firstMouse = packet(241)
        val secondMouse = packet(241)

        mailbox.enqueue(fifo)
        mailbox.enqueue(firstMouse)
        mailbox.enqueue(secondMouse)

        val first = mailbox.pollNext()
        val second = mailbox.pollNext()

        assertSame(fifo, first?.packet())
        assertEquals(InboundPacketMailbox.Family.FIFO, first?.family())
        assertEquals(0, firstMouse.payload.refCnt())
        assertSame(secondMouse, second?.packet())
        assertEquals(InboundPacketMailbox.Family.MOUSE, second?.family())

        release(first?.packet())
        release(second?.packet())
    }

    @Test
    fun `inbound mailbox drops fifo packets at capacity and reports counters`() {
        val mailbox = InboundPacketMailbox(1)
        val first = packet(30)
        val second = packet(31)

        val acceptedFirst = mailbox.enqueue(first)
        val acceptedSecond = mailbox.enqueue(second)
        val counters = mailbox.snapshotAndResetCounters()

        assertTrue(acceptedFirst.accepted())
        assertFalse(acceptedSecond.accepted())
        assertEquals(1, counters.fifoDropped())

        release(mailbox.pollNext()?.packet())
        release(second)
    }

    @Test
    fun `inbound mailbox clear releases all queued packets and resets pending count`() {
        val mailbox = InboundPacketMailbox(8)
        val fifo = packet(40)
        val walk = packet(248)
        val mouse = packet(241)

        mailbox.enqueue(fifo)
        mailbox.enqueue(walk)
        mailbox.enqueue(mouse)
        mailbox.clear { packet -> release(packet) }

        assertEquals(0, mailbox.pendingCount())
        assertEquals(0, fifo.payload.refCnt())
        assertEquals(0, walk.payload.refCnt())
        assertEquals(0, mouse.payload.refCnt())
        assertNull(mailbox.pollNext())
    }

    @Test
    fun `outbound session queue drains in fifo order and counts bytes`() {
        val queue = OutboundSessionQueue()
        val first = message(1, 2, 3)
        val second = message(4, 5)
        val channel = EmbeddedChannel()

        queue.enqueue(first)
        queue.enqueue(second)

        val drain = queue.drainTo(channel)
        channel.flush()

        assertEquals(2, drain.messageCount())
        assertEquals(5, drain.byteCount())

        val outboundFirst = channel.readOutbound<ByteMessage>()
        val outboundSecond = channel.readOutbound<ByteMessage>()
        assertSame(first, outboundFirst)
        assertSame(second, outboundSecond)

        outboundFirst.releaseAll()
        outboundSecond.releaseAll()
        channel.finishAndReleaseAll()
    }

    @Test
    fun `outbound session queue empty drain reuses shared zero result`() {
        val queue = OutboundSessionQueue()
        val channel = EmbeddedChannel()

        val first = queue.drainTo(channel)
        val second = queue.drainTo(channel)

        assertSame(first, second)
        assertEquals(0, first.messageCount())
        assertEquals(0, first.byteCount())

        channel.finishAndReleaseAll()
    }

    @Test
    fun `outbound session queue release all releases queued messages`() {
        val queue = OutboundSessionQueue()
        val first = message(1, 2, 3)
        val second = message(4, 5)

        queue.enqueue(first)
        queue.enqueue(second)
        queue.releaseAll()

        assertEquals(0, first.refCnt())
        assertEquals(0, second.refCnt())
        assertTrue(queue.isEmpty())
    }

    private fun packet(opcode: Int): GamePacket {
        val payload = Unpooled.buffer(4)
        payload.writeByte(opcode)
        return GamePacket(opcode, payload.writerIndex(), payload)
    }

    private fun message(vararg values: Int): ByteMessage {
        val buffer = Unpooled.buffer(values.size)
        for (value in values) {
            buffer.writeByte(value)
        }
        return ByteMessage.wrap(buffer)
    }

    private fun release(packet: GamePacket?) {
        val payload = packet?.payload ?: return
        if (payload.refCnt() > 0) {
            payload.release()
        }
    }
}
