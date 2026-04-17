package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HighRiskInboundListenerContractTest {
    @Test
    fun `trade and duel request listeners decode validate and delegate only`() {
        val tradeSource =
            Files.readString(Path.of("src/main/java/net/dodian/uber/game/netty/listener/in/TradeRequestListener.java"))
        val duelSource =
            Files.readString(Path.of("src/main/java/net/dodian/uber/game/netty/listener/in/DuelRequestListener.java"))

        assertTrue(tradeSource.contains("PacketInteractionRequestService.handleLegacyTradeRequest(client, targetSlot, other)"))
        assertFalse(tradeSource.contains("client.tradeReq("))
        assertFalse(tradeSource.contains("client.duelReq("))
        assertFalse(tradeSource.contains("client.openTrade("))

        assertTrue(duelSource.contains("PacketInteractionRequestService.handleDuelRequest(client, pid, other)"))
        assertFalse(duelSource.contains("client.duelReq("))
        assertFalse(duelSource.contains("client.openTrade("))
    }

    @Test
    fun `walking and appearance listeners delegate gameplay mutations to services`() {
        val walkingSource =
            Files.readString(Path.of("src/main/java/net/dodian/uber/game/netty/listener/in/WalkingListener.java"))
        val appearanceSource =
            Files.readString(Path.of("src/main/java/net/dodian/uber/game/netty/listener/in/ChangeAppearanceListener.java"))

        assertTrue(walkingSource.contains("PacketGameplayFacade.handleWalk(client, request);"))
        assertFalse(walkingSource.contains("client.declineTrade("))
        assertFalse(walkingSource.contains("client.declineDuel("))
        assertFalse(walkingSource.contains("client.resetWalkingQueue("))

        assertTrue(appearanceSource.contains("PacketAppearanceService.handleAppearanceChange(client, looks);"))
        assertFalse(appearanceSource.contains("client.setLook("))
        assertFalse(appearanceSource.contains("UpdateFlag.APPEARANCE"))
    }

    @Test
    fun `dialogue listener posts event and avoids direct dialogue side effects`() {
        val source =
            Files.readString(Path.of("src/main/java/net/dodian/uber/game/netty/listener/in/DialogueListener.java"))

        assertTrue(source.contains("GameEventBus.post(new DialogueContinueEvent(client));"))
        assertFalse(source.contains("client.showNPCChat("))
        assertFalse(source.contains("DialogueService.onContinue("))
        assertFalse(source.contains("client.send("))
    }
}
