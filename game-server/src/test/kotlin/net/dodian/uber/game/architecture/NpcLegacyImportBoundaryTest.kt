package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NpcLegacyImportBoundaryTest {
    @Test
    fun `non compatibility main sources do not reference legacy npc package`() {
        val sourceRoot = Paths.get("src/main/kotlin/net/dodian/uber/game")
        val violations = mutableListOf<String>()
        val legacyToken = "net.dodian.uber.game.content.npcs"
        val explicitCompatFiles = setOf(
            "/api/plugin/ContentModuleIndex.kt",
        )

        Files.walk(sourceRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "kt" }
                .forEach { file ->
                    val normalized = file.toString().replace('\\', '/')
                    if (normalized.contains("/content/npcs/")) return@forEach
                    if (explicitCompatFiles.any { normalized.endsWith(it) }) return@forEach

                    Files.readAllLines(file).forEachIndexed { idx, line ->
                        if (line.contains(legacyToken)) {
                            violations += "${file}:${idx + 1} -> ${line.trim()}"
                        }
                    }
                }
        }

        assertTrue(
            violations.isEmpty(),
            "Legacy npc package references must remain compatibility-only.\n${violations.joinToString("\n")}",
        )
    }
}
