package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.streams.toList
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventWiringParityTest {
    private val sourceRoot: Path = Paths.get("src/main")

    private data class SourceFileSnapshot(
        val path: Path,
        val lines: List<String>,
    )

    private val sourceFiles: List<SourceFileSnapshot> by lazy {
        Files.walk(sourceRoot)
            .filter { Files.isRegularFile(it) }
            .filter { it.extension == "kt" || it.extension == "java" }
            .map { file ->
                SourceFileSnapshot(
                    path = file,
                    lines = Files.readAllLines(file),
                )
            }
            .toList()
    }

    @Test
    fun `core events have at least one producer callsite`() {
        val expectedProducers = setOf(
            "ItemDropEvent",
            "NpcDeathEvent",
            "PlayerLoginEvent",
            "PlayerLogoutEvent",
            "PlayerDeathEvent",
            "WorldTickEvent",
        )

        val missing = mutableListOf<String>()
        expectedProducers.forEach { eventName ->
            val found = sourceFiles.any { snapshot -> hasProducer(snapshot.lines, eventName) }
            if (!found) {
                missing += "$eventName -> expected a code-like producer callsite"
            }
        }

        assertTrue(
            missing.isEmpty(),
            "Missing event producer callsites:\n${missing.joinToString("\n")}",
        )
    }

    private fun hasProducer(lines: List<String>, eventName: String): Boolean {
        val eventPattern = Regex("""\b(?:new\s+)?${Regex.escape(eventName)}\s*\(""")
        return lines.any { rawLine ->
            val line = rawLine.trim()
            if (isCommentOrBlank(line)) {
                return@any false
            }
            (line.contains("GameEventBus.post(") ||
                line.contains("GameEventBus.postWithResult(") ||
                line.contains("GameEventBus.postAndReturn(") ||
                line.contains("new $eventName(")) &&
                eventPattern.containsMatchIn(line)
        }
    }

    private fun isCommentOrBlank(line: String): Boolean {
        if (line.isBlank()) {
            return true
        }
        return line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")
    }
}
