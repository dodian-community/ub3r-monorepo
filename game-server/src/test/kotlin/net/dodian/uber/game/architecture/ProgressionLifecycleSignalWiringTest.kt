package net.dodian.uber.game.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ProgressionLifecycleSignalWiringTest {
    private val sourceRoot: Path = Paths.get("src/main/kotlin")

    @Test
    fun `level-up event is defined in flat events package`() {
        val eventsFile =
            sourceRoot.resolve("net/dodian/uber/game/events/ProgressionLifecycleEvents.kt")
        assertTrue(Files.exists(eventsFile), "ProgressionLifecycleEvents.kt must exist in net.dodian.uber.game.events")

        val content = Files.readString(eventsFile)
        assertTrue(content.contains("package net.dodian.uber.game.events"))
        assertTrue(content.contains("data class LevelUpEvent("))
    }

    @Test
    fun `death event is defined in flat events package`() {
        val eventsFile =
            sourceRoot.resolve("net/dodian/uber/game/events/ProgressionLifecycleEvents.kt")
        assertTrue(Files.exists(eventsFile), "ProgressionLifecycleEvents.kt must exist in net.dodian.uber.game.events")

        val content = Files.readString(eventsFile)
        assertTrue(content.contains("package net.dodian.uber.game.events"))
        assertTrue(content.contains("data class DeathEvent("))
    }

    @Test
    fun `skill progression posts level-up event on level transitions`() {
        val progressionFile =
            sourceRoot.resolve("net/dodian/uber/game/content/skills/core/progression/SkillProgressionService.kt")
        val content = Files.readString(progressionFile)

        assertTrue(
            content.contains("GameEventBus.post(") && content.contains("LevelUpEvent("),
            "SkillProgressionService must post LevelUpEvent when levels increase",
        )
    }

    @Test
    fun `player death tick service posts death event when death begins`() {
        val deathServiceFile =
            sourceRoot.resolve("net/dodian/uber/game/engine/lifecycle/PlayerDeathTickService.kt")
        val content = Files.readString(deathServiceFile)

        assertTrue(
            content.contains("GameEventBus.post(DeathEvent("),
            "PlayerDeathTickService must post DeathEvent when death sequence starts",
        )
    }
}
