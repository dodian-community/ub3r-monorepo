package net.dodian.uber.social.dueling

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DuelingModuleTest {
    @Test fun `declares duel lifecycle ownership`() {
        assertTrue(DuelingModule.contentManifest.declaredRouteKeys.any { it.contains("close-move-logout-death") })
    }
}
