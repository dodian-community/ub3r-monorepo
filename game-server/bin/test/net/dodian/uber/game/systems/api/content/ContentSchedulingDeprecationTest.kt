package net.dodian.uber.game.systems.api.content

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentSchedulingDeprecationTest {
    @Test
    fun `legacy content coroutine facade file is removed`() {
        assertFalse(
            Files.exists(Paths.get("src/main/kotlin/net/dodian/uber/game/systems/api/content/ContentCoroutines.kt")),
        )
    }

    @Test
    fun `legacy tick tasks facade file is removed`() {
        assertFalse(
            Files.exists(Paths.get("src/main/kotlin/net/dodian/uber/game/tasks/TickTasks.kt")),
        )
    }

    @Test
    fun `no source imports removed tick tasks facade`() {
        val roots =
            listOf(
                Paths.get("src/main/kotlin/net/dodian/uber/game"),
                Paths.get("src/test/kotlin/net/dodian/uber/game"),
            )
        val fqcn = "net.dodian.uber.game.tasks.TickTasks"
        val importLine = "import $fqcn"
        val violations = mutableListOf<String>()

        roots.forEach { root ->
            if (!Files.exists(root)) {
                return@forEach
            }
            Files.walk(root).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .forEach { file ->
                        Files.readAllLines(file).forEachIndexed { idx, line ->
                            if (line.trim() == importLine) {
                                violations += "${file}:${idx + 1}"
                            }
                        }
                    }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Found forbidden source references to removed TickTasks facade:\n${violations.joinToString("\n")}",
        )
    }
}
