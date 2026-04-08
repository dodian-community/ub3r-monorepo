package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class TaskingSurfaceAreaBoundaryTest {
    @Test
    fun `content runtime api does not depend on TickTasks directly`() {
        val contentRuntimeApi = Files.readString(
            Paths.get("src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentRuntimeApi.kt"),
        )
        assertFalse(contentRuntimeApi.contains("TickTasks"))
    }
}
