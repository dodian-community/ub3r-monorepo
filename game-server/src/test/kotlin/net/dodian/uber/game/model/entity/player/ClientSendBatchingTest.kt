package net.dodian.uber.game.model.entity.player

import io.netty.channel.embedded.EmbeddedChannel
import java.util.concurrent.CountDownLatch
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.runtime.loop.GameThreadContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ClientSendBatchingTest {

    @AfterEach
    fun tearDown() {
        GameThreadContext.clearBindingForTests()
    }

    @Test
    fun `send uses write on game thread and flushes on outbound flush`() {
        val channel = EmbeddedChannel()
        val client = Client(channel, 1)
        GameThreadContext.bindCurrentThread()

        client.send(ByteMessage.raw())
        assertNull(channel.readOutbound<Any>())

        client.flushOutbound()
        val flushed = channel.readOutbound<Any>()
        assertNotNull(flushed)
        (flushed as ByteMessage).releaseAll()
        channel.finishAndReleaseAll()
    }

    @Test
    fun `send uses writeAndFlush off game thread`() {
        val channel = EmbeddedChannel()
        val client = Client(channel, 2)
        GameThreadContext.bindCurrentThread()
        val latch = CountDownLatch(1)

        Thread {
            client.send(ByteMessage.raw())
            latch.countDown()
        }.start()
        latch.await()

        val flushed = channel.readOutbound<Any>()
        assertNotNull(flushed)
        (flushed as ByteMessage).releaseAll()
        channel.finishAndReleaseAll()
    }
}
