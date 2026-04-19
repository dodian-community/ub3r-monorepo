package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacyCachePackageBoundaryTest {
    @Test
    fun `legacy java cache package is removed`() {
        val root = Paths.get("src/main/java/net/dodian/cache")
        if (!Files.exists(root)) {
            return
        }

        val javaFiles = Files.walk(root).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.extension == "java" }
                .toList()
        }

        assertTrue(
            javaFiles.isEmpty(),
            "Legacy Java cache files must be removed. Found: ${javaFiles.joinToString(",")}",
        )
    }

    @Test
    fun `source does not import removed cache internals`() {
        val root = Paths.get("src/main")
        val forbiddenImports = listOf(
            "import net.dodian.cache.Cache",
            "import net.dodian.cache.Archive",
            "import net.dodian.cache.index.",
            "import net.dodian.cache.map.",
            "import net.dodian.cache.obj.",
            "import net.dodian.cache.region.Region",
            "import net.dodian.utilities.Rangable",
        )
        val violations = mutableListOf<String>()

        if (Files.exists(root)) {
            Files.walk(root).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) && (it.extension == "kt" || it.extension == "java") }
                    .forEach { file ->
                        Files.readAllLines(file).forEachIndexed { index, line ->
                            val trimmed = line.trim()
                            if (forbiddenImports.any { forbidden -> trimmed.startsWith(forbidden) }) {
                                violations += "${file}:${index + 1} -> $trimmed"
                            }
                        }
                    }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Removed cache internals must not be imported.\n${violations.joinToString("\n")}",
        )
    }
}
