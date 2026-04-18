package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StateOwnershipBoundaryTest {
    @Test
    fun `state ownership policy declares high-risk runtime domains`() {
        val source = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/state/StateOwnershipPolicy.kt"))
        assertTrue(source.contains("SESSION_PACKET_QUEUE"))
        assertTrue(source.contains("INTERACTION_SESSION"))
        assertTrue(source.contains("TASK_ACTION_STATE"))
        assertTrue(source.contains("ownershipByDomain"))
    }

    @Test
    fun `netty listeners do not directly own interaction or task state fields`() {
        val root = Path.of("src/main/java/net/dodian/uber/game/netty/listener/in")
        val forbidden = listOf(
            "tradeConfirmed =",
            "tradeConfirmed2 =",
            "duelConfirmed =",
            "duelConfirmed2 =",
            "canOffer =",
            "playerTaskSet =",
            "npcTaskSet =",
            "pendingInteraction =",
        )
        val violations = mutableListOf<String>()

        Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "java" }
                .forEach { file ->
                    Files.readAllLines(file).forEachIndexed { idx, line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                            return@forEachIndexed
                        }
                        if (forbidden.any { token -> trimmed.contains(token) }) {
                            violations += "$file:${idx + 1} -> $trimmed"
                        }
                    }
                }
        }

        assertTrue(
            violations.isEmpty(),
            "Netty listener edge must not directly mutate interaction/task state.\n${violations.joinToString("\n")}",
        )
    }
}
