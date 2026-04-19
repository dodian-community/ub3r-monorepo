package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoroutineLifecycleBoundaryTest {
    private val kotlinRoot = Path.of("src/main/kotlin")

    @Test
    fun `global scope is not used in production sources`() {
        val violations = mutableListOf<String>()
        Files.walk(kotlinRoot).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .forEach { file ->
                    Files.readAllLines(file).forEachIndexed { index, line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("//")) {
                            return@forEachIndexed
                        }
                        if (trimmed.contains("GlobalScope")) {
                            violations += "$file:${index + 1}"
                        }
                    }
                }
        }

        assertTrue(
            violations.isEmpty(),
            "GlobalScope is forbidden; use lifecycle-managed scopes instead.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `player scoped coroutine lifecycle bootstrap is wired to logout event`() {
        val bootstrapPath =
            Path.of("src/main/kotlin/net/dodian/uber/game/engine/event/bootstrap/PlayerScopedCoroutineLifecycleBootstrap.kt")
        val source = Files.readString(bootstrapPath)

        assertTrue(source.contains("GameEventBus.on<PlayerLogoutEvent>"))
        assertTrue(source.contains("PlayerScopedCoroutineService.cancelForPlayer("))
    }
}
