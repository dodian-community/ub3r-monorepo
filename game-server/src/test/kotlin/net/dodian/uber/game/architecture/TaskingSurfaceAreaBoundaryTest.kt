package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskingSurfaceAreaBoundaryTest {
    @Test
    fun `content runtime api does not depend on TickTasks directly`() {
        val contentRuntimeApi = Files.readString(
            Paths.get("src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentRuntimeApi.kt"),
        )
        assertFalse(contentRuntimeApi.contains("TickTasks"))
    }

    @Test
    fun `runtime and service code does not depend on TickTasks facade`() {
        val runtimeRoot = Paths.get("src/main/kotlin/net/dodian/uber/game")
        val allowedFacade = runtimeRoot.resolve("tasks/TickTasks.kt").toAbsolutePath().normalize()
        val violations = mutableListOf<String>()

        Files.walk(runtimeRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "kt" }
                .forEach { file ->
                    val normalized = file.toAbsolutePath().normalize()
                    if (normalized == allowedFacade) {
                        return@forEach
                    }
                    Files.readAllLines(file).forEachIndexed { idx, line ->
                        val trimmed = line.trim()
                        if (trimmed.contains("TickTasks")) {
                            violations += "${file}:${idx + 1} -> $trimmed"
                        }
                    }
                }
        }

        assertTrue(
            violations.isEmpty(),
            "Runtime/service code must not depend on TickTasks outside the legacy facade.\n${violations.joinToString("\n")}",
        )
    }
}
