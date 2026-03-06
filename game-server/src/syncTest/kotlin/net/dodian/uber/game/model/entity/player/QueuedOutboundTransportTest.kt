package net.dodian.uber.game.model.entity.player

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.netty.codec.ByteMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class QueuedOutboundTransportTest {

    @Test
    fun `active loaded clients queue outbound until tick flush`() {
        val channel = EmbeddedChannel()
        val client = Client(channel, 1)
        client.isActive = true
        client.loaded = true

        client.send(ByteMessage.raw())
        client.send(ByteMessage.raw())

        assertNull(channel.readOutbound<Any>())

        val stats = client.flushOutbound()
        assertEquals(2, stats.flushedMessages)

        release(channel.readOutbound())
        release(channel.readOutbound())
        assertNull(channel.readOutbound<Any>())
        channel.finishAndReleaseAll()
    }

    @Test
    fun `pre game clients still flush immediately`() {
        val channel = EmbeddedChannel()
        val client = Client(channel, 2)
        client.isActive = true
        client.loaded = false

        client.send(ByteMessage.raw())

        val outbound = channel.readOutbound<Any>()
        assertNotNull(outbound)
        release(outbound)
        channel.finishAndReleaseAll()
    }

    private fun release(message: Any?) {
        if (message is ByteMessage) {
            message.releaseAll()
        }
    }
}
