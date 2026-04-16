package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DialogueDslBoundaryTest {
    private val roots =
        listOf(
            Path.of("src/main/kotlin/net/dodian/uber/game/npc"),
            Path.of("src/main/kotlin/net/dodian/uber/game/skill"),
        )

    @Test
    fun `dialogue option branches do not mix legacy chat actions with finish closeInterfaces false`() {
        val mixedPattern =
            Regex(
                pattern =
                    """action\s*\{[^}]{0,500}(?:showNPCChat|showPlayerChat)\s*\([^)]*\)[^}]{0,500}\}\s*[\r\n\s]*finish\(\s*closeInterfaces\s*=\s*false""",
                options = setOf(RegexOption.DOT_MATCHES_ALL),
            )

        val violations = mutableListOf<String>()
        roots.filter(Files::exists).forEach { root ->
            Files.walk(root).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .forEach { file ->
                        val source = Files.readString(file)
                        if (mixedPattern.containsMatchIn(source)) {
                            violations += file.toString()
                        }
                    }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Dialogue branches must use session DSL chat steps only. Found mixed legacy chat + finish(closeInterfaces=false) in:\n${violations.joinToString("\n")}",
        )
    }
}
