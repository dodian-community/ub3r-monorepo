package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeathEventNamingBoundaryTest {
    @Test
    fun `player death tick posts player death event not generic death event`() {
        val source =
            Files.readString(
                Paths.get("src/main/kotlin/net/dodian/uber/game/engine/lifecycle/PlayerDeathTickService.kt"),
            )

        assertTrue(source.contains("PlayerDeathEvent("))
        assertFalse(source.contains("events.DeathEvent"))
        assertFalse(source.contains("GameEventBus.post(DeathEvent"))
    }
}
