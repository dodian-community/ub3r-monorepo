package net.dodian.uber.game.architecture

import io.netty.channel.embedded.EmbeddedChannel
import java.nio.file.Files
import java.nio.file.Path
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InfraBehaviorRegressionTest {
    @Test
    fun `examine listener preserves packet to event to fallback chain`() {
        val source = Files.readString(Path.of("src/main/java/net/dodian/uber/game/netty/listener/in/ExamineListener.java"))

        assertTrue(source.contains("GameEventBus.postWithResult(new ItemExamineEvent"))
        assertTrue(source.contains("GameEventBus.postWithResult(new NpcExamineEvent"))
        assertTrue(source.contains("GameEventBus.postWithResult(new ObjectExamineEvent"))
        assertTrue(source.contains("client.examineItem(client, ID, posX)"))
        assertTrue(source.contains("client.examineNpc(client, ID)"))
        assertTrue(source.contains("client.examineObject(client, ID, objectPosition)"))
    }

    @Test
    fun `trade duel transition routing remains delegated through runtime services`() {
        val tradeSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/ui/TradeInterface.kt"))
        val duelSource = Files.readString(Path.of("src/main/kotlin/net/dodian/uber/game/ui/DuelInterface.kt"))
        val sessionSource =
            Files.readString(
                Path.of("src/main/kotlin/net/dodian/uber/game/engine/systems/interaction/ui/TradeDuelSessionService.kt"),
            )

        assertTrue(tradeSource.contains("TradeDuelSessionService.confirmTradeStageOne"))
        assertTrue(tradeSource.contains("TradeDuelSessionService.confirmTradeStageTwo"))
        assertTrue(duelSource.contains("TradeDuelSessionService.confirmDuelStageOne"))
        assertTrue(duelSource.contains("TradeDuelSessionService.confirmDuelStageTwo"))
        assertTrue(sessionSource.contains("TradeDuelStateMachine.advanceTradeStageOne"))
        assertTrue(sessionSource.contains("TradeDuelStateMachine.advanceDuelStageTwo"))
    }

    @Test
    fun `player task helpers preserve tick based timing invariants`() {
        GameTaskRuntime.clear()
        val client = client(1911)
        val events = mutableListOf<String>()

        GameTaskRuntime.queueSkillAction(client, "timing") {
            events += "skill-start"
            wait(2)
            events += "skill-end"
        }

        GameTaskRuntime.queueDialogueStep(client, "dialogue") {
            events += "dialogue-start"
            wait(1)
            events += "dialogue-end"
        }

        repeat(5) { GameTaskRuntime.cyclePlayer(client) }

        assertEquals(listOf("skill-start", "skill-end", "dialogue-start", "dialogue-end"), events)
    }

    private fun client(slot: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.playerName = "infra-regression-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.validClient = true
        client.teleportTo(3096, 3490, 0)
        return client
    }
}
