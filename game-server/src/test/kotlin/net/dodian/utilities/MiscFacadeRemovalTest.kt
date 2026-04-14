package net.dodian.utilities

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MiscFacadeRemovalTest {
    private val forbiddenFqcn = listOf("net", "dodian", "utilities", "Misc").joinToString(".")

    @Test
    fun `legacy misc facade file is removed`() {
        assertFalse(
            Files.exists(Path.of("src/main/kotlin/net/dodian/utilities/Misc.kt")),
            "Legacy net.dodian.utilities.Misc facade should be removed.",
        )
    }

    @Test
    fun `no source references removed misc facade`() {
        val roots =
            listOf(
                Path.of("src/main/kotlin/net/dodian"),
                Path.of("src/main/java/net/dodian"),
                Path.of("src/test/kotlin/net/dodian"),
                Path.of("src/test/java/net/dodian"),
            )
        val forbiddenImport = "import $forbiddenFqcn"
        val forbiddenStaticUsagePrefix = "$forbiddenFqcn."
        val violations = mutableListOf<String>()

        roots.filter(Files::exists).forEach { root ->
            Files.walk(root).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) && (it.toString().endsWith(".kt") || it.toString().endsWith(".java")) }
                    .forEach { file ->
                        Files.readAllLines(file).forEachIndexed { idx, line ->
                            val trimmed = line.trim()
                            if (trimmed == forbiddenImport || line.contains(forbiddenStaticUsagePrefix)) {
                                violations += "${file}:${idx + 1}"
                            }
                        }
                    }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Found forbidden source references to removed Misc facade:\n${violations.joinToString("\n")}",
        )
    }
}

