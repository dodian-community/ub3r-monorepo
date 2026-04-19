package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoreRuntimeExceptionCatchGuardTest {
    @Test
    fun `core runtime kotlin source does not use broad catch exception`() {
        val violations = mutableListOf<String>()
        for (path in coreRuntimeTargets()) {
            if (Files.isDirectory(path)) {
                Files.walk(path).use { stream ->
                    stream
                        .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                        .forEach { file ->
                            val source = Files.readString(file)
                            if (BROAD_EXCEPTION_REGEX.containsMatchIn(source)) {
                                violations += file.toString()
                            }
                        }
                }
            } else if (Files.isRegularFile(path)) {
                val source = Files.readString(path)
                if (BROAD_EXCEPTION_REGEX.containsMatchIn(source)) {
                    violations += path.toString()
                }
            }
        }
        assertTrue(
            violations.isEmpty(),
            "Broad catch(Exception) found in core runtime targets: ${violations.joinToString()}",
        )
    }

    @Test
    fun `queued packet catch path records listener exception telemetry with player context`() {
        val source = Files.readString(Path.of("src/main/java/net/dodian/uber/game/model/entity/player/Client.java"))
        val telemetry = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/metrics/PacketErrorTelemetry.kt"))

        assertTrue(source.contains("PacketRejectTelemetry.record(packet.opcode(), PacketRejectReason.LISTENER_EXCEPTION)"))
        assertTrue(source.contains("PacketErrorTelemetry.recordListenerException("))
        assertTrue(source.contains("describeRecentInboundPackets()"))
        assertTrue(source.contains("player={} slot={} opcode={} size={} recent={}"))
        assertTrue(telemetry.contains("packet.listener.exception.total"))
        assertTrue(telemetry.contains("packet.listener.exception.opcode."))
        assertTrue(telemetry.contains("packet.listener.exception.player."))
    }

    private fun coreRuntimeTargets(): List<Path> =
        listOf(
            Path.of("src/main/kotlin/net/dodian/uber/game/engine"),
            Path.of("src/main/kotlin/net/dodian/uber/game/persistence/account"),
            Path.of("src/main/kotlin/net/dodian/uber/game/persistence/audit"),
            Path.of("src/main/kotlin/net/dodian/uber/game/persistence/world"),
            Path.of("src/main/kotlin/net/dodian/uber/game/persistence/world/WorldSavePublisher.kt"),
            Path.of("src/main/kotlin/net/dodian/uber/game/persistence/db/Database.kt"),
            Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/world"),
            Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/skills/SkillInteractionDispatcher.kt"),
            Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/objects/ObjectInteractionService.kt"),
            Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/npcs/NpcContentDispatcher.kt"),
            Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/items/ItemDispatcher.kt"),
            Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/ObjectClipService.kt"),
        )

    private companion object {
        private val BROAD_EXCEPTION_REGEX = Regex("""catch\s*\(\s*(?:\w+|_)\s*:\s*Exception\s*\)""")
    }
}
