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
    private val skillsRoot: Path = Paths.get("src/main/kotlin/net/dodian/uber/game/content/skills")
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
            "content/skills/cooking/Cooking.kt",
            "content/skills/crafting/Crafting.kt",
            "content/skills/fishing/Fishing.kt",
            "content/skills/fletching/Fletching.kt",
            "content/skills/mining/Mining.kt",
            "content/skills/prayer/Prayer.kt",
            "content/skills/smithing/Smithing.kt",
            "content/skills/woodcutting/Woodcutting.kt",
        )

        runtimeOwners.forEach { relativePath ->
            val fullPath = kotlinSourceRoot.resolve("net/dodian/uber/game/$relativePath")
            assertTrue(Files.exists(fullPath), "Expected runtime owner file to exist: $relativePath")
        }
    }

    @Test
    fun `runtime owned skills expose normalized action api`() {
        val apiOwners = listOf(
            "content/skills/cooking/Cooking.kt",
            "content/skills/crafting/Crafting.kt",
            "content/skills/fishing/Fishing.kt",
            "content/skills/fletching/Fletching.kt",
            "content/skills/prayer/Prayer.kt",
            "content/skills/smithing/Smithing.kt",
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
}

