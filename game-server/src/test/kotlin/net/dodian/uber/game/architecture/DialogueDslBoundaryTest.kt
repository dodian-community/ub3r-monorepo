package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DialogueDslBoundaryTest {
    private val npcRoot = Path.of("src/main/kotlin/net/dodian/uber/game/npc")
    private val roots =
        listOf(
            npcRoot,
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

    @Test
    fun `high risk dialogue modules avoid legacy chat api entirely`() {
        val guardedModules =
            listOf(
                Path.of("src/main/kotlin/net/dodian/uber/game/npc/SlayerMasterDialogue.kt"),
                Path.of("src/main/kotlin/net/dodian/uber/game/npc/HerbloreNpcDialogue.kt"),
                Path.of("src/main/kotlin/net/dodian/uber/game/npc/Saniboch.kt"),
                Path.of("src/main/kotlin/net/dodian/uber/game/npc/TzhaarMejJal.kt"),
            )

        val violations =
            guardedModules
                .filter(Files::exists)
                .filter { path ->
                    val source = Files.readString(path)
                    source.contains("showNPCChat(") || source.contains("showPlayerChat(")
                }
                .map(Path::toString)

        assertTrue(
            violations.isEmpty(),
            "Guarded dialogue modules must use DSL session steps only; remove legacy showNPCChat/showPlayerChat calls.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `npc modules avoid legacy chat api except explicit legacy registry exceptions`() {
        val allowedLegacyUsage =
            setOf(
                Path.of("src/main/kotlin/net/dodian/uber/game/npc/DukeHoracio.kt"),
            )

        val violations = mutableListOf<String>()
        Files.walk(npcRoot).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .forEach { file ->
                    val source = Files.readString(file)
                    val hasLegacyCalls = source.contains("showNPCChat(") || source.contains("showPlayerChat(")
                    if (hasLegacyCalls && file !in allowedLegacyUsage) {
                        violations += file.toString()
                    }
                }
        }

        assertTrue(
            violations.isEmpty(),
            "NPC modules should use DSL dialogue session steps; direct showNPCChat/showPlayerChat is blocked outside explicit legacy exceptions.\n${violations.joinToString("\n")}",
        )
    }
}
