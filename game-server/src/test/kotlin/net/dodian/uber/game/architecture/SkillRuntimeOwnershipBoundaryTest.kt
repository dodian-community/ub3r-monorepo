package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkillRuntimeOwnershipBoundaryTest {
    private val kotlinSourceRoot: Path = Paths.get("src/main/kotlin")
    private val skillsRoot: Path = Paths.get("src/main/kotlin/net/dodian/uber/game/skill")
    private val retiredServiceFile: Path =
        Paths.get("src/main/kotlin/net/dodian/uber/game/systems/action/SkillingActionService.kt")

    @Test
    fun `retired skilling action service file is removed`() {
        assertFalse(Files.exists(retiredServiceFile), "SkillingActionService should be retired and removed.")
    }

    @Test
    fun `skills do not reference retired skilling action service`() {
        val references = Files.walk(skillsRoot).use { paths ->
            paths.iterator().asSequence()
                .filter { Files.isRegularFile(it) }
                .filter { it.extension == "kt" }
                .mapNotNull { file ->
                    val content = Files.readString(file)
                    if (content.contains("SkillingActionService")) {
                        file.toString()
                    } else {
                        null
                    }
                }
                .toList()
        }

        assertTrue(
            references.isEmpty(),
            "Skills should own runtime loops directly. Found retired service references in:\n${references.joinToString("\n")}",
        )
    }

    @Test
    fun `runtime action ownership lives under skill content package`() {
        val runtimeOwners = listOf(
            "skill/cooking/Cooking.kt",
            "skill/crafting/Crafting.kt",
            "skill/fishing/Fishing.kt",
            "skill/fletching/Fletching.kt",
            "skill/mining/Mining.kt",
            "skill/prayer/Prayer.kt",
            "skill/smithing/Smithing.kt",
            "skill/woodcutting/Woodcutting.kt",
        )

        runtimeOwners.forEach { relativePath ->
            val fullPath = kotlinSourceRoot.resolve("net/dodian/uber/game/$relativePath")
            assertTrue(Files.exists(fullPath), "Expected runtime owner file to exist: $relativePath")
        }
    }

    @Test
    fun `runtime owned skills expose normalized action api`() {
        val apiOwners = listOf(
            "skill/cooking/Cooking.kt",
            "skill/crafting/Crafting.kt",
            "skill/fishing/Fishing.kt",
            "skill/fletching/Fletching.kt",
            "skill/prayer/Prayer.kt",
            "skill/smithing/Smithing.kt",
        )

        val missing = mutableListOf<String>()
        apiOwners.forEach { relativePath ->
            val fullPath = kotlinSourceRoot.resolve("net/dodian/uber/game/$relativePath")
            val content = Files.readString(fullPath)
            if (!content.contains("fun startAction(")) {
                missing += "$relativePath missing fun startAction(...)"
            }
            if (!content.contains("fun stopAction(")) {
                missing += "$relativePath missing fun stopAction(...)"
            }
            if (!content.contains("fun stopFromReset(client: Client, fullReset: Boolean)")) {
                missing += "$relativePath missing normalized stopFromReset signature"
            }
        }

        assertTrue(
            missing.isEmpty(),
            "Runtime API naming/signature drift detected:\n${missing.joinToString("\n")}",
        )
    }

    @Test
    fun `skill modules avoid direct legacy player flags and counters`() {
        val forbiddenPatterns = listOf(
            ".UsingAgility" to Regex("""\.UsingAgility\b"""),
            ".agilityCourseStage" to Regex("""\.agilityCourseStage\b"""),
            ".slayerData[" to Regex("""\.slayerData\s*\["""),
            ".playerPotato" to Regex("""\.playerPotato\b"""),
            ".chestEvent" to Regex("""\.chestEvent\b"""),
            ".chestEventOccur" to Regex("""\.chestEventOccur\b"""),
            ".randomed" to Regex("""\.randomed\b"""),
            ".random_skill" to Regex("""\.random_skill\b"""),
        )

        val violations = Files.walk(skillsRoot).use { paths ->
            paths.iterator().asSequence()
                .filter { Files.isRegularFile(it) }
                .filter { it.extension == "kt" || it.extension == "java" }
                .flatMap { file ->
                    val content = Files.readString(file)
                    forbiddenPatterns
                        .filter { (_, pattern) -> pattern.containsMatchIn(content) }
                        .map { (token, _) -> "${file}: references forbidden legacy token `$token`" }
                        .asSequence()
                }
                .toList()
        }

        assertTrue(
            violations.isEmpty(),
            "Skill modules must use typed runtime state adapters, not direct legacy player fields.\n${violations.joinToString("\n")}",
        )
    }

    @Test
    fun `skill dsl exposes session lifecycle helpers`() {
        val dslPath = kotlinSourceRoot.resolve("net/dodian/uber/game/api/plugin/skills/SkillPluginDsl.kt")
        val source = Files.readString(dslPath)

        val missing = buildList {
            if (!source.contains("fun startSession(")) add("startSession")
            if (!source.contains("fun requireSession(")) add("requireSession")
            if (!source.contains("fun endSession(")) add("endSession")
        }

        assertTrue(
            missing.isEmpty(),
            "SkillPlugin DSL missing required lifecycle helper(s): ${missing.joinToString(", ")}",
        )
    }
}
