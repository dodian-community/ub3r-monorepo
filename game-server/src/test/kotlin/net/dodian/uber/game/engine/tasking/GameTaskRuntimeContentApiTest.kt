package net.dodian.uber.game.engine.tasking

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameTaskRuntimeContentApiTest {
    @Test
    fun `runtime exposes content-facing queue helper APIs with metadata tagging`() {
        val runtimeSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/tasking/GameTaskRuntime.kt"))
        val taskSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/tasking/GameTask.kt"))

        assertTrue(runtimeSource.contains("fun queueSkillAction("))
        assertTrue(runtimeSource.contains("actionName: String"))
        assertTrue(runtimeSource.contains("fun queueNpcAction("))
        assertTrue(runtimeSource.contains("fun queueDialogueStep("))
        assertTrue(runtimeSource.contains("setMetadata(\"skillAction\", actionName)"))
        assertTrue(runtimeSource.contains("setMetadata(\"npcAction\", actionName)"))
        assertTrue(runtimeSource.contains("setMetadata(\"dialogueStep\", stepName)"))

        assertTrue(taskSource.contains("private val metadata = HashMap<String, String>()"))
        assertTrue(taskSource.contains("fun setMetadata(key: String, value: String)"))
        assertTrue(taskSource.contains("fun metadataSnapshot(): Map<String, String> = metadata.toMap()"))
    }
}
