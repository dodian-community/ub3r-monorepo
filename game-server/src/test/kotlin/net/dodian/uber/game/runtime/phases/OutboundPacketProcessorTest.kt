package net.dodian.uber.game.runtime.phases

import io.netty.channel.embedded.EmbeddedChannel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OutboundPacketProcessorTest {
    @Test
    fun `legacy outbound updates players then flushes zone then drains outbound then clears flags`() {
        val events = CopyOnWriteArrayList<String>()
        val active = TestClient(1, events, "active").apply { isActive = true }
        val snapshots = ArrayDeque<List<Client>>().apply {
            add(listOf(active))
            add(listOf(active))
        }
        val processor =
            OutboundPacketProcessor(
                syncEnabledProvider = { false },
                activePlayersSnapshot = { snapshots.removeFirst() },
                zoneFlush = { players ->
                    events += "zoneFlush:${players.joinToString(",") { it.playerName }}"
                },
                npcFlagClearer = { events += "npcClear" },
                removePlayer = { error("removePlayer should not be called") },
            )

        processor.run()

        assertEquals(
            listOf("update:active", "zoneFlush:active", "flush:active", "npcClear", "playerClear:active"),
            events,
        )
    }

    @Test
    fun `legacy outbound removes disconnected players before update`() {
        val events = CopyOnWriteArrayList<String>()
        val disconnected = TestClient(2, events, "gone").apply {
            isActive = true
            disconnected = true
        }
        val snapshots = ArrayDeque<List<Client>>().apply {
            add(listOf(disconnected))
            add(emptyList())
        }
        val removed = AtomicBoolean(false)
        val processor =
            OutboundPacketProcessor(
                syncEnabledProvider = { false },
                activePlayersSnapshot = { snapshots.removeFirst() },
                zoneFlush = { events += "zoneFlush:${it.size}" },
                npcProvider = { emptyList() },
                removePlayer = {
                    removed.set(true)
                    events += "remove:${it.playerName}"
                },
            )

        processor.run()

        assertTrue(removed.get())
        assertEquals(listOf("remove:gone", "zoneFlush:0"), events)
    }

    @Test
    fun `sync enabled delegates to synchronization runner and skips legacy path`() {
        val syncRan = AtomicBoolean(false)
        val legacyTouched = AtomicBoolean(false)
        val processor =
            OutboundPacketProcessor(
                syncEnabledProvider = { true },
                syncRunner = { syncRan.set(true) },
                activePlayersSnapshot = {
                    legacyTouched.set(true)
                    emptyList()
                },
                zoneFlush = { legacyTouched.set(true) },
                npcProvider = {
                    legacyTouched.set(true)
                    emptyList()
                },
            )

        processor.run()

        assertTrue(syncRan.get())
        assertEquals(false, legacyTouched.get())
    }

    private class TestClient(
        slot: Int,
        private val events: MutableList<String>,
        name: String,
    ) : Client(EmbeddedChannel(), slot) {
        init {
            playerName = name
            connectedFrom = "127.0.0.1"
        }

        override fun update() {
            events += "update:$playerName"
        }

        override fun flushOutbound(): OutboundFlushStats {
            events += "flush:$playerName"
            return OutboundFlushStats.empty()
        }

        override fun clearUpdateFlags() {
            events += "playerClear:$playerName"
        }

        override fun println_debug(str: String) = Unit
    }
}
