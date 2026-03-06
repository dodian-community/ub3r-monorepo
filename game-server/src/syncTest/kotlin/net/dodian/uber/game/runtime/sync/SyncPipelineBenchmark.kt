package net.dodian.uber.game.runtime.sync

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.item.ItemManager
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.netty.game.GamePacket
import io.netty.buffer.Unpooled
import net.dodian.uber.game.runtime.sync.cache.RootSynchronizationCache
import net.dodian.uber.game.runtime.sync.player.PlayerChunkActivityIndex
import net.dodian.uber.game.runtime.sync.player.PlayerSyncRevisionIndex
import net.dodian.uber.game.runtime.sync.playerinfo.RootPlayerInfoService
import kotlin.system.measureNanoTime

object SyncPipelineBenchmark {
    @JvmStatic
    fun main(args: Array<String>) {
        runScenario(300)
        runScenario(500)
        runIdleCompatibilityScenario()
    }

    private fun runScenario(players: Int) {
        val clients =
            ArrayList<Client>(players).apply {
                for (slot in 1..players) {
                    add(
                        Client(EmbeddedChannel(), slot).apply {
                            isActive = true
                            loaded = true
                        },
                    )
                }
            }

        val inboundNanos =
            measureNanoTime {
                clients.forEach { client ->
                    repeat(8) { index ->
                        client.queueInboundPacket(GamePacket(248, 1, Unpooled.buffer(1).writeByte(index)))
                    }
                    repeat(8) { index ->
                        client.queueInboundPacket(GamePacket(255, 1, Unpooled.buffer(1).writeByte(index)))
                    }
                    client.processQueuedPackets(16)
                }
            }

        val outboundNanos =
            measureNanoTime {
                clients.forEach { client ->
                    repeat(12) {
                        client.send(ByteMessage.raw())
                    }
                    client.flushOutbound()
                }
            }

        println(
            "players=$players inboundMs=${inboundNanos / 1_000_000L} outboundMs=${outboundNanos / 1_000_000L}",
        )
    }

    private fun runIdleCompatibilityScenario() {
        val originalUpdateRunning = Server.updateRunning
        val originalItemManager = Server.itemManager
        Server.updateRunning = false
        try {
            Server.itemManager = testItemManager()
            val players =
                listOf(createSyncClient(1), createSyncClient(2)).onEach { client ->
                    PlayerHandler.players[client.slot] = client
                }

            runRootSync(players)
            players.forEach(::drainOutbound)

            var idlePackets = 0
            val nanos =
                measureNanoTime {
                    repeat(200) {
                        runRootSync(players)
                        players.forEach { player ->
                            player.flushOutbound()
                            val channel = player.channel as EmbeddedChannel
                            while (true) {
                                val outbound = channel.readOutbound<Any>() ?: break
                                if (outbound is ByteMessage) {
                                    idlePackets++
                                    outbound.releaseAll()
                                }
                            }
                        }
                    }
                }

            println("idleCompatibility ticks=200 players=${players.size} idlePackets=$idlePackets totalMs=${nanos / 1_000_000L}")
            players.forEach { player ->
                PlayerHandler.players[player.slot] = null
                (player.channel as EmbeddedChannel).finishAndReleaseAll()
            }
        } finally {
            Server.updateRunning = originalUpdateRunning
            Server.itemManager = originalItemManager
        }
    }

    private fun createSyncClient(slot: Int): Client =
        Client(EmbeddedChannel(), slot).apply {
            isActive = true
            loaded = true
            setPlayerName("bench-$slot")
            moveTo(3200 + slot, 3200, 0)
        }

    private fun runRootSync(players: List<Client>): SynchronizationCycle {
        val revisionIndex = PlayerSyncRevisionIndex()
        val activityIndex = PlayerChunkActivityIndex()
        revisionIndex.rebuild(players, 1L, activityIndex)
        val cycle =
            SynchronizationCycle(
                tick = 1L,
                rootCache = RootSynchronizationCache(),
                viewportIndex = null,
                playerRevisionIndex = revisionIndex,
                playerActivityIndex = activityIndex,
            )
        SynchronizationContext.setCurrent(cycle)
        try {
            RootPlayerInfoService.INSTANCE.sync(players)
            return cycle
        } finally {
            SynchronizationContext.clear()
        }
    }

    private fun drainOutbound(client: Client) {
        client.flushOutbound()
        val channel = client.channel as EmbeddedChannel
        while (true) {
            val outbound = channel.readOutbound<Any>() ?: break
            if (outbound is ByteMessage) {
                outbound.releaseAll()
            }
        }
    }

    private fun testItemManager(): ItemManager =
        object : ItemManager() {
            override fun loadGlobalItems() = Unit
            override fun loadItems() = Unit
        }
}
