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
        val sessionSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/ui/TradeDuelSessionService.kt"))
        val machineSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/ui/TradeDuelStateMachine.kt"))

        assertTrue(tradeSource.contains("TradeDuelSessionService.confirmTradeStageOne("))
        assertTrue(tradeSource.contains("TradeDuelSessionService.confirmTradeStageTwo("))
        assertFalse(tradeSource.contains("client.tradeConfirmed ="))
        assertFalse(tradeSource.contains("client.tradeConfirmed2 ="))

        assertTrue(duelSource.contains("TradeDuelSessionService.confirmDuelStageOne("))
        assertTrue(duelSource.contains("TradeDuelSessionService.confirmDuelStageTwo("))
        assertFalse(duelSource.contains("client.duelConfirmed ="))
        assertFalse(duelSource.contains("client.duelConfirmed2 ="))

        assertTrue(sessionSource.contains("TradeDuelStateMachine.advanceTradeStageOne("))
        assertTrue(sessionSource.contains("TradeDuelStateMachine.advanceTradeStageTwo("))
        assertTrue(sessionSource.contains("TradeDuelStateMachine.advanceDuelStageOne("))
        assertTrue(sessionSource.contains("TradeDuelStateMachine.advanceDuelStageTwo("))
        assertTrue(machineSource.contains("fun advanceTradeStageOne(client: Client, other: Client): Boolean"))
        assertTrue(machineSource.contains("fun advanceTradeStageTwo(client: Client, other: Client): Boolean"))
        assertTrue(machineSource.contains("fun advanceDuelStageOne(client: Client, other: Client): Boolean"))
        assertTrue(machineSource.contains("fun advanceDuelStageTwo(client: Client, other: Client): Boolean"))
    }

    @Test
    fun `movement and interface close paths delegate trade duel cancellations to runtime service`() {
        val walkingSource =
            Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketWalkingService.kt"))
        val closeSource =
            Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/net/PacketInterfaceCloseService.kt"))
        val clientSource =
            Files.readString(Path.of("src/main/java/net/dodian/uber/game/model/entity/player/Client.java"))

        assertTrue(walkingSource.contains("TradeDuelSessionService.closeOpenTrade(player)"))
        assertTrue(walkingSource.contains("TradeDuelSessionService.closeOpenDuel(player)"))
        assertFalse(walkingSource.contains("player.declineTrade()"))
        assertFalse(walkingSource.contains("player.declineDuel()"))

        assertTrue(closeSource.contains("TradeDuelSessionService.closeOpenTrade(client)"))
        assertTrue(closeSource.contains("TradeDuelSessionService.closeOpenDuel(client)"))
        assertFalse(closeSource.contains("client.declineTrade()"))
        assertFalse(closeSource.contains("client.declineDuel()"))

        assertTrue(clientSource.contains("TradeDuelSessionService.closeOnLogout(this)"))
    }
}
