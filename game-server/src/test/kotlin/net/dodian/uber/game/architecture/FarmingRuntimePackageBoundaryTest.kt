package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FarmingRuntimePackageBoundaryTest {
    @Test
    fun `canonical farming runtime file exists in skills domain`() {
        assertTrue(
            Files.exists(
                Paths.get("src/main/kotlin/net/dodian/uber/game/systems/skills/farming/runtime/FarmingRuntimeService.kt"),
            ),
            "Expected canonical farming runtime service file to exist",
        )
    }
}
