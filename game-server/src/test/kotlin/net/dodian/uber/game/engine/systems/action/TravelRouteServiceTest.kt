package net.dodian.uber.game.engine.systems.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TravelRouteServiceTest {
    @Test
    fun `home route refuses non catherby selection`() {
        val decision = TravelRouteService.resolve(home = true, checkPos = 0, buttonId = 3058) { true }
        assertEquals(TravelDecision.Rejected("Please select Catherby!"), decision)
    }

    @Test
    fun `locked non-home route requests unlock dialogue`() {
        val decision = TravelRouteService.resolve(home = false, checkPos = 0, buttonId = 3059) { false }
        assertEquals(TravelDecision.RequireUnlockDialogue(48054), decision)
    }
}
