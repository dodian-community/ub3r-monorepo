package net.dodian.uber.game.runtime.sync

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.netty.game.GamePacket
import io.netty.buffer.Unpooled
import kotlin.system.measureNanoTime

object SyncPipelineBenchmark {
    @JvmStatic
    fun main(args: Array<String>) {
        runScenario(300)
        runScenario(500)
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
}
