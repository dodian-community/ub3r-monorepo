package net.dodian.uber.game.content.commands

import io.netty.channel.embedded.EmbeddedChannel
import java.util.concurrent.atomic.AtomicInteger
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandDispatcherTest {
    @AfterEach
    fun resetRegistry() {
        CommandContentRegistry.resetForTests()
    }

    @Test
    fun `dispatcher resolves custom alias case insensitively`() {
        val invoked = AtomicInteger(0)
        CommandContentRegistry.resetForTests(
            object : CommandContent {
                override fun definitions() =
                    commands {
                        command("zz_test_dispatch") {
                            invoked.incrementAndGet()
                            true
                        }
                    }
            },
        )

        val client = Client(EmbeddedChannel(), 1).apply { validClient = true }

        val handled = CommandDispatcher.dispatch(client, "ZZ_TEST_DISPATCH arg")

        assertTrue(handled)
        assertEquals(1, invoked.get())
        (client.channel as EmbeddedChannel).finishAndReleaseAll()
    }

    @Test
    fun `registry bootstraps split command modules`() {
        assertFalse(CommandContentRegistry.definitionsFor("players").isEmpty())
        assertFalse(CommandContentRegistry.definitionsFor("yell").isEmpty())
        assertFalse(CommandContentRegistry.definitionsFor("bosspawn").isEmpty())
        assertFalse(CommandContentRegistry.definitionsFor("tele").isEmpty())
    }

    @Test
    fun `registry preserves duplicate aliases in split modules`() {
        assertEquals(2, CommandContentRegistry.definitionsFor("pnpc").size)
        assertEquals(2, CommandContentRegistry.definitionsFor("rehp").size)
        assertEquals(2, CommandContentRegistry.definitionsFor("bank").size)
    }
}
