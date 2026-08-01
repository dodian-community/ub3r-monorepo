package net.dodian.uber.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StartupPresentationTest {
    @Test
    fun `startup banner preserves attribution as a single logger-friendly line`() {
        assertEquals(
            "Dodian Ub3r starting - maintained by Dodian.net",
            Server.startupBanner(),
        )
    }
}
