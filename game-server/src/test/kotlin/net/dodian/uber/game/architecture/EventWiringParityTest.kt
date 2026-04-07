package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventWiringParityTest {
    private val sourceRoot: Path = Paths.get("src/main")

    private val sourceFiles: List<Path> by lazy {
        Files.walk(sourceRoot)
            .filter { Files.isRegularFile(it) }
            .filter { it.extension == "kt" || it.extension == "java" }
            .toList()
    }

    @Test
    fun `core events have at least one producer callsite`() {
        val expectedProducers = mapOf(
            "ItemDropEvent" to listOf("new ItemDropEvent("),
            "ItemPickupEvent" to listOf("new ItemPickupEvent("),
            "NpcDeathEvent" to listOf("new NpcDeathEvent("),
            "NpcDropEvent" to listOf("new NpcDropEvent("),
            "TradeRequestEvent" to listOf("new TradeRequestEvent("),
            "TradeCancelEvent" to listOf("new TradeCancelEvent("),
            "TradeCompleteEvent" to listOf("new TradeCompleteEvent("),
            "PlayerLoginEvent" to listOf("new PlayerLoginEvent("),
            "PlayerLogoutEvent" to listOf("new PlayerLogoutEvent("),
            "PlayerDeathEvent" to listOf("PlayerDeathEvent("),
            "WorldTickEvent" to listOf("WorldTickEvent("),
        )

        val missing = mutableListOf<String>()
        expectedProducers.forEach { (eventName, markers) ->
            val found = sourceFiles.any { file ->
                val content = Files.readString(file)
                markers.any { marker -> content.contains(marker) }
            }
            if (!found) {
                missing += "$eventName -> expected one of: ${markers.joinToString()}"
            }
        }

        assertTrue(
            missing.isEmpty(),
            "Missing event producer callsites:\n${missing.joinToString("\n")}",
        )
    }
}

