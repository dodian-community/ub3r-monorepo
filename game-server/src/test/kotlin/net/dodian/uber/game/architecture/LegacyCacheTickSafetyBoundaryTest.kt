package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacyCacheTickSafetyBoundaryTest {
    @Test
    fun `legacy cache shim package has no blocking io or sleeps`() {
        val root = Paths.get("src/main/kotlin/net/dodian/cache")
        val forbidden = listOf("Thread.sleep(", "RandomAccessFile(", "FileInputStream(", "executeQuery(", "executeUpdate(")
        val violations = mutableListOf<String>()

        if (Files.exists(root)) {
            Files.walk(root).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) && (it.extension == "kt" || it.extension == "java") }
                    .forEach { file ->
                        Files.readAllLines(file).forEachIndexed { index, line ->
                            if (forbidden.any { token -> line.contains(token) }) {
                                violations += "${file}:${index + 1} -> ${line.trim()}"
                            }
                        }
                    }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Shim must stay non-blocking.\n${violations.joinToString("\n")}",
        )
    }
}
