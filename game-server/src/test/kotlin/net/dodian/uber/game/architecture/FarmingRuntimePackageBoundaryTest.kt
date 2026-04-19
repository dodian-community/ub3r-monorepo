package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FarmingRuntimePackageBoundaryTest {
    @Test
    fun `canonical farming runtime file exists in skills domain`() {
        assertTrue(
            Files.exists(
                Paths.get("src/main/kotlin/net/dodian/uber/game/engine/systems/skills/farming/runtime/FarmingRuntimeService.kt"),
            ),
            "Expected canonical farming runtime service file to exist",
        )
    }

    @Test
    fun `legacy world farming runtime shims are removed`() {
        val legacyFiles = listOf(
            "src/main/kotlin/net/dodian/uber/game/systems/world/farming/FarmingRuntimeService.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/world/farming/FarmingPersistenceCodec.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/world/farming/FarmingRuntimeModels.kt",
            "src/main/kotlin/net/dodian/uber/game/systems/world/farming/FarmingRunStats.kt",
            "src/main/kotlin/net/dodian/uber/game/engine/systems/world/farming/FarmingRuntimeService.kt",
            "src/main/kotlin/net/dodian/uber/game/engine/systems/world/farming/FarmingPersistenceCodec.kt",
            "src/main/kotlin/net/dodian/uber/game/engine/systems/world/farming/FarmingRuntimeModels.kt",
            "src/main/kotlin/net/dodian/uber/game/engine/systems/world/farming/FarmingRunStats.kt",
        )
        legacyFiles.forEach { path ->
            assertFalse(Files.exists(Paths.get(path)), "Legacy farming shim should be removed: $path")
        }
    }
}
