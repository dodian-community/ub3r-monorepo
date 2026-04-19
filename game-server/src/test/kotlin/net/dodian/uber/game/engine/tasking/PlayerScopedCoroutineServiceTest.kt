package net.dodian.uber.game.engine.tasking

import io.netty.channel.embedded.EmbeddedChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerScopedCoroutineServiceTest {
    @Test
    fun `launch with same key replaces previous job for player`() =
        runBlocking {
            val client = client(1901)
            val firstCancelled = CompletableDeferred<Boolean>()
            val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

            val first =
                PlayerScopedCoroutineService.launch(client, "refund-check", testScope) {
                    try {
                        while (isActive) {
                            delay(25L)
                        }
                    } finally {
                        firstCancelled.complete(!isActive)
                    }
                }

            val replacement =
                PlayerScopedCoroutineService.launch(client, "refund-check", testScope) {
                    delay(10L)
                }

            withTimeout(2_000L) {
                first.join()
                replacement.join()
            }
            testScope.coroutineContext[Job]?.cancel()

            assertTrue(first.isCancelled, "Expected prior keyed job to be cancelled")
            assertTrue(firstCancelled.await(), "Expected replaced job to observe cancellation")
            assertEquals(0, PlayerScopedCoroutineService.activeJobCount(client))
        }

    @Test
    fun `cancel for player cancels all keyed jobs`() =
        runBlocking {
            val client = client(1902)
            val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val first =
                PlayerScopedCoroutineService.launch(client, "job-a", testScope) {
                    while (isActive) {
                        delay(25L)
                    }
                }
            val second =
                PlayerScopedCoroutineService.launch(client, "job-b", testScope) {
                    while (isActive) {
                        delay(25L)
                    }
                }

            assertEquals(2, PlayerScopedCoroutineService.activeJobCount(client))
            PlayerScopedCoroutineService.cancelForPlayer(client, "test-cancel")

            withTimeout(2_000L) {
                first.join()
                second.join()
            }
            testScope.coroutineContext[Job]?.cancel()

            assertTrue(first.isCancelled)
            assertTrue(second.isCancelled)
            assertEquals(0, PlayerScopedCoroutineService.activeJobCount(client))
        }

    private fun client(slot: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.playerName = "scoped-coro-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.validClient = true
        client.teleportTo(3096, 3490, 0)
        return client
    }
}
