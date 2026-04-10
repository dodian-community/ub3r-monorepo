package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NoRangableArchitectureTest {
    @Test
    fun `source tree no longer references rangable`() {
        val root = Paths.get("src/main")
        val violations = mutableListOf<String>()

        if (Files.exists(root)) {
            Files.walk(root).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) && (it.extension == "kt" || it.extension == "java") }
                    .forEach { file ->
                        Files.readAllLines(file).forEachIndexed { index, line ->
                            if (RANGABLE_TOKEN.containsMatchIn(line)) {
                                violations += "${file}:${index + 1} -> ${line.trim()}"
                            }
                        }
                    }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Rangable references must be removed from src/main.\n${violations.joinToString("\n")}",
        )
    }

    private companion object {
        val RANGABLE_TOKEN = Regex("\\bRangable\\b")
    }
}
