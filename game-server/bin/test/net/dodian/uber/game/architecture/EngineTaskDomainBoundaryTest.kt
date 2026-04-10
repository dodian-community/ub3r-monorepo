package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EngineTaskDomainBoundaryTest {
    @Test
    fun `content packages do not import engine tasking or sync internals`() {
        val contentRoot = Paths.get("src/main/kotlin/net/dodian/uber/game/content")
        val violations = mutableListOf<String>()
        val forbiddenPrefixes = listOf(
            "import net.dodian.uber.game.engine.tasking",
            "import net.dodian.uber.game.engine.sync",
            "import net.dodian.uber.game.engine.phases",
            "import net.dodian.uber.game.engine.net",
        )

        Files.walk(contentRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "kt" }
                .forEach { file ->
                    Files.readAllLines(file).forEachIndexed { idx, line ->
                        val trimmed = line.trim()
                        if (forbiddenPrefixes.any { trimmed.startsWith(it) }) {
                            violations += "${file}:${idx + 1} -> $trimmed"
                        }
                    }
                }
        }

        assertTrue(
            violations.isEmpty(),
            "Content task domain must not import engine internals.\n${violations.joinToString("\n")}",
        )
    }
}
