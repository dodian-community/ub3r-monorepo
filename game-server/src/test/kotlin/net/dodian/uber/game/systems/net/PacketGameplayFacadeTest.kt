package net.dodian.uber.game.systems.net

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `walk handling fails fast until task 3 wires the real service`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            PacketGameplayFacade.handleWalk(null, null)
        }
        assertTrue(
            exception.message?.contains("not wired yet") == true,
            "Expected a clear migration message, got: ${exception.message}",
        )
    }
}
