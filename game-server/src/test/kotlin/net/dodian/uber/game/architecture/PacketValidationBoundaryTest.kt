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

    @Test
    fun `interaction listeners use shared reject reasons and strict payload bounds`() {
        val listenerFiles =
            listOf(
                Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/ObjectInteractionListener.java"),
                Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/NpcInteractionListener.java"),
                Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/ExamineListener.java"),
            )

        val missingReasonContract =
            listenerFiles.filter { path ->
                val source = Files.readString(path)
                !source.contains("PacketRejectReason")
            }
        val missingUpperBoundChecks =
            listenerFiles.filter { path ->
                val source = Files.readString(path)
                !source.contains("readableBytes() >")
            }

        assertTrue(
            missingReasonContract.isEmpty(),
            "Listeners must use PacketRejectReason. Missing:\n${missingReasonContract.joinToString("\n")}",
        )
        assertTrue(
            missingUpperBoundChecks.isEmpty(),
            "Listeners must enforce max payload checks with readableBytes() > ... Missing:\n${missingUpperBoundChecks.joinToString("\n")}",
        )
    }
}
