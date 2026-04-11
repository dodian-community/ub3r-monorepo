package net.dodian.uber.game.api.content

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.npc.NpcManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentTaskRecipesTest {
    @Test
    fun `world countdown emits ticks in descending order and completes once`() {
        GameTaskRuntime.clear()
        val ticks = mutableListOf<Int>()
        var completed = 0

        val handle =
            ContentTaskRecipes.worldCountdown(totalTicks = 3, onTick = { remaining ->
                ticks += remaining
            }) {
                completed++
            }

        repeat(6) { GameTaskRuntime.cycleWorld() }

        assertEquals(listOf(3, 2, 1), ticks)
        assertEquals(1, completed)
        assertTrue(handle.isCompleted())
    }

    @Test
    fun `player countdown emits ticks in descending order and completes once`() {
        GameTaskRuntime.clear()
        val player = Client(EmbeddedChannel(), 1)
        val ticks = mutableListOf<Int>()
        var completed = 0

        val handle =
            ContentTaskRecipes.playerCountdown(player = player, totalTicks = 3, onTick = { remaining ->
                ticks += remaining
            }) {
                completed++
            }

        repeat(6) { GameTaskRuntime.cyclePlayer(player) }

        assertEquals(listOf(3, 2, 1), ticks)
        assertEquals(1, completed)
        assertTrue(handle.isCompleted())
    }

    @Test
    fun `npc countdown emits ticks in descending order and completes once`() {
        GameTaskRuntime.clear()
        val previousNpcManager = Server.npcManager
        try {
            Server.npcManager = previousNpcManager ?: NpcManager()
            val npc = Npc(1, 1, Position(3200, 3200, 0), 0)
            val ticks = mutableListOf<Int>()
            var completed = 0

            val handle =
                ContentTaskRecipes.npcCountdown(npc = npc, totalTicks = 3, onTick = { remaining ->
                    ticks += remaining
                }) {
                    completed++
                }

            repeat(6) { GameTaskRuntime.cycleNpc(npc) }

            assertEquals(listOf(3, 2, 1), ticks)
            assertEquals(1, completed)
            assertTrue(handle.isCompleted())
        } finally {
            Server.npcManager = previousNpcManager
        }
    }

    @Test
    fun `world retry until completes when action succeeds`() {
        GameTaskRuntime.clear()
        var attempts = 0
        var giveUps = 0

        val handle =
            ContentTaskRecipes.worldRetryUntil(intervalTicks = 1, initialDelayTicks = 0, maxAttempts = 5, action = {
                attempts++
                attempts >= 3
            }) {
                giveUps++
            }

        repeat(8) { GameTaskRuntime.cycleWorld() }

        assertEquals(3, attempts)
        assertEquals(0, giveUps)
        assertTrue(handle.isCompleted())
    }

    @Test
    fun `world retry until gives up once when max attempts exhausted`() {
        GameTaskRuntime.clear()
        var attempts = 0
        var giveUps = 0

        val handle =
            ContentTaskRecipes.worldRetryUntil(intervalTicks = 1, maxAttempts = 3, action = {
                attempts++
                false
            }) {
                giveUps++
            }

        repeat(8) { GameTaskRuntime.cycleWorld() }

        assertEquals(3, attempts)
        assertEquals(1, giveUps)
        assertTrue(handle.isCompleted())
    }
}
