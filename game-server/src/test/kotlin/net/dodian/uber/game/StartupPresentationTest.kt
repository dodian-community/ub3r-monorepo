package net.dodian.uber.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StartupPresentationTest {
    @Test
    fun `startup banner preserves branding and attribution`() {
        assertEquals(
            listOf(
                "    ____",
                "   / __ \\____ ____/ (_)___ _____",
                "  / / / / __ \\/ __ / / __ `/ __ \\",
                " / /_/ / /_/ / /_/ / / /_/ / / / /",
                "/_____/\\____/\\____/_/\\____/_/ /_/",
                "",
                "Maintained by Dodian.net",
            ).joinToString("\n"),
            Server.startupBanner(),
        )
    }
}
