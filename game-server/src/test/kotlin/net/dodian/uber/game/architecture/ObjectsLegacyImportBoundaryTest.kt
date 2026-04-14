package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObjectsLegacyImportBoundaryTest {
    private val legacyRootPrefix = listOf("net", "dodian", "uber", "game", "content", "objects").joinToString(".")

    @Test
    fun `legacy content objects source trees are removed`() {
        assertFalse(Files.exists(Path.of("src/main/kotlin/net/dodian/uber/game/content/objects")))
        assertFalse(Files.exists(Path.of("src/test/kotlin/net/dodian/uber/game/content/objects")))
    }

    @Test
    fun `no source imports legacy content objects packages`() {
        val roots =
            listOf(
                Path.of("src/main/kotlin/net/dodian"),
                Path.of("src/main/java/net/dodian"),
                Path.of("src/test/kotlin/net/dodian"),
                Path.of("src/test/java/net/dodian"),
            )
        val violations = mutableListOf<String>()

        roots.filter(Files::exists).forEach { root ->
            Files.walk(root).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) && (it.toString().endsWith(".kt") || it.toString().endsWith(".java")) }
                    .forEach { file ->
                        Files.readAllLines(file).forEachIndexed { idx, line ->
                            val trimmed = line.trim()
                            if (trimmed.startsWith("import ") && trimmed.contains(legacyRootPrefix)) {
                                violations += "${file}:${idx + 1}"
                            }
                        }
                    }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Found forbidden imports from retired content.objects packages:\n${violations.joinToString("\n")}",
        )
    }
}
