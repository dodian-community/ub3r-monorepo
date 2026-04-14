package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentSchedulerImportBoundaryTest {
    @Test
    fun `content package does not import GameEventScheduler directly`() {
        val contentRoot = Paths.get("src/main/kotlin/net/dodian/uber/game/content")
        val violations = mutableListOf<String>()

        if (!Files.exists(contentRoot)) {
            return
        }

        Files.walk(contentRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "kt" }
                .forEach { file ->
                    Files.readAllLines(file).forEachIndexed { index, line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("import net.dodian.uber.game.engine.event.GameEventScheduler")) {
                            violations += "${file}:${index + 1} -> $trimmed"
                        }
                    }
                }
        }

        assertTrue(
            violations.isEmpty(),
            "Content must not import GameEventScheduler directly.\n${violations.joinToString("\n")}",
        )
    }
}
