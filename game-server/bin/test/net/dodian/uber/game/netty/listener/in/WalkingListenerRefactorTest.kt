package net.dodian.uber.game.netty.listener.`in`

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class WalkingListenerRefactorTest {
    @Test
    fun `walking listener delegates walk gameplay to kotlin service`() {
        val source = Files.readString(
            Paths.get("src/main/java/net/dodian/uber/game/netty/listener/in/WalkingListener.java"),
        )

        assertTrue(
            source.contains("PacketGameplayFacade.handleWalk("),
            "Expected WalkingListener to route through PacketGameplayFacade.handleWalk",
        )
        assertFalse(source.contains("client.declineTrade("))
        assertFalse(source.contains("client.declineDuel("))
        assertFalse(source.contains("DialogueService.closeBlockingDialogue("))
        assertFalse(source.contains("PlayerActionCancellationService.cancel("))
        assertFalse(source.contains("client.rerequestAnim("))
        assertFalse(source.contains("client.resetWalkingQueue("))
        assertFalse(source.contains("client.faceTarget("))
        assertFalse(source.contains("client.send(new RemoveInterfaces()"))
        assertFalse(source.contains("client.checkItemUpdate("))
    }
}
