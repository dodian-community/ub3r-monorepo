package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentTaskApiBoundaryTest {
    @Test
    fun `content packages do not depend on low-level scheduler symbols`() {
        val contentRoot = Paths.get("src/main/kotlin/net/dodian/uber/game/content")
        val violations = mutableListOf<String>()
        val forbiddenTokens = listOf(
            "GameTaskRuntime",
            "GameEventScheduler",
            "TickTasks",
            "worldTaskCoroutine(",
            "playerTaskCoroutine(",
            "npcTaskCoroutine(",
        )

        Files.walk(contentRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "kt" }
                .forEach { file ->
                    Files.readAllLines(file).forEachIndexed { idx, line ->
                        val trimmed = line.trim()
                        if (forbiddenTokens.any { trimmed.contains(it) }) {
                            violations += "${file}:${idx + 1} -> $trimmed"
                        }
                    }
                }
        }

        assertTrue(
            violations.isEmpty(),
            "Content task domain must use systems.api.content surface.\n${violations.joinToString("\n")}",
        )
    }
}
