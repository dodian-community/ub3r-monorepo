package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TradeDuelTransitionBoundaryTest {
    @Test
    fun `packet interaction request service delegates trade and duel transitions to runtime service`() {
        val source =
            Files.readString(
                Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketInteractionRequestService.kt"),
            )

        assertTrue(source.contains("TradeDuelSessionService.requestTrade(client, targetSlot)"))
        assertTrue(source.contains("TradeDuelSessionService.requestDuel(client, pid)"))
        assertTrue(source.contains("TradeDuelSessionService.requestLegacyTrade(client, targetSlot)"))
        assertFalse(source.contains("client.tradeReq("))
        assertFalse(source.contains("client.duelReq("))
    }

    @Test
    fun `trade and duel ui route handlers delegate state transitions to runtime service`() {
        val tradeSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/ui/TradeInterface.kt"))
        val duelSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/ui/DuelInterface.kt"))

        assertTrue(tradeSource.contains("TradeDuelSessionService.confirmTradeStageOne("))
        assertTrue(tradeSource.contains("TradeDuelSessionService.confirmTradeStageTwo("))
        assertFalse(tradeSource.contains("client.tradeConfirmed ="))
        assertFalse(tradeSource.contains("client.tradeConfirmed2 ="))

        assertTrue(duelSource.contains("TradeDuelSessionService.confirmDuelStageOne("))
        assertTrue(duelSource.contains("TradeDuelSessionService.confirmDuelStageTwo("))
        assertFalse(duelSource.contains("client.duelConfirmed ="))
        assertFalse(duelSource.contains("client.duelConfirmed2 ="))
    }

    @Test
    fun `movement and interface close paths delegate trade duel cancellations to runtime service`() {
        val walkingSource =
            Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketWalkingService.kt"))
        val closeSource =
            Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketInterfaceCloseService.kt"))

        assertTrue(walkingSource.contains("TradeDuelSessionService.closeOpenTrade(player)"))
        assertTrue(walkingSource.contains("TradeDuelSessionService.closeOpenDuel(player)"))
        assertFalse(walkingSource.contains("player.declineTrade()"))
        assertFalse(walkingSource.contains("player.declineDuel()"))

        assertTrue(closeSource.contains("TradeDuelSessionService.closeOpenTrade(client)"))
        assertTrue(closeSource.contains("TradeDuelSessionService.closeOpenDuel(client)"))
        assertFalse(closeSource.contains("client.declineTrade()"))
        assertFalse(closeSource.contains("client.declineDuel()"))
    }
}
