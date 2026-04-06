package net.dodian.uber.game.systems.net

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class PacketGameplayFacadeTest {
    @Test
    fun `walk request captures decoded packet state`() {
        val request = WalkRequest(
            opcode = 248,
            firstStepXAbs = 3200,
            firstStepYAbs = 3201,
            running = true,
            deltasX = intArrayOf(1, 0, -1),
            deltasY = intArrayOf(0, 1, 0),
        )

        assertEquals(248, request.opcode)
        assertEquals(3200, request.firstStepXAbs)
        assertEquals(3201, request.firstStepYAbs)
        assertTrue(request.running)
        assertEquals(3, request.deltasX.size)
        assertEquals(3, request.deltasY.size)
    }

    @Test
    fun `walk handling routes through the facade dispatcher`() {
        val request = WalkRequest(
            opcode = 248,
            firstStepXAbs = 3200,
            firstStepYAbs = 3201,
            running = true,
            deltasX = intArrayOf(0),
            deltasY = intArrayOf(0),
        )

        val previousDispatcher = PacketGameplayFacade.walkDispatcher
        try {
            var observedRequest: WalkRequest? = null
            PacketGameplayFacade.walkDispatcher = { _, routedRequest ->
                observedRequest = routedRequest
            }

            PacketGameplayFacade.handleWalk(Client(EmbeddedChannel(), 1), request)

            assertSame(request, observedRequest, "Expected handleWalk to forward the original WalkRequest")
        } finally {
            PacketGameplayFacade.walkDispatcher = previousDispatcher
        }
    }
}
