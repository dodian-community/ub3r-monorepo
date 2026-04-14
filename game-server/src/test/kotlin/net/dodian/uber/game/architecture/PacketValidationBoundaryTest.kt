package net.dodian.uber.game.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class PacketValidationBoundaryTest {
    @Test
    fun `high risk listeners include malformed packet rejection telemetry`() {
        val listenerFiles =
            listOf(
                Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/NpcInteractionListener.java"),
                Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/ObjectInteractionListener.java"),
                Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/ItemOnItemListener.java"),
                Paths.get("src/main/java/net/dodian/uber/game/netty/game/GamePacketDecoder.java"),
                Paths.get("src/main/java/net/dodian/uber/game/netty/game/GamePacketHandler.java"),
            )
        val violations =
            listenerFiles.mapNotNull { path ->
                val source = Files.readString(path)
                if (!source.contains("PacketRejectTelemetry")) {
                    path.toString()
                } else {
                    null
                }
            }
        assertTrue(
            violations.isEmpty(),
            "Packet reject telemetry must be present in high-risk decode/queue surfaces.\n${violations.joinToString("\n")}",
        )
    }
}
