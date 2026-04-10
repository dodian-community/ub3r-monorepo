package net.dodian.uber.game.systems.api.content

import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContentSchedulingTest {
    @Test
    fun `world delay executes on expected tick`() {
        GameTaskRuntime.clear()
        val events = mutableListOf<String>()

        ContentScheduling.world {
            events += "start"
            delayTicks(2)
            events += "after-2"
        }

        repeat(4) { GameTaskRuntime.cycleWorld() }

        assertEquals(listOf("start", "after-2"), events)
    }

    @Test
    fun `world repeating stops when block returns false`() {
        GameTaskRuntime.clear()
        val events = mutableListOf<Int>()

        ContentScheduling.worldRepeating(intervalTicks = 1) {
            events += events.size
            events.size < 3
        }

        repeat(6) { GameTaskRuntime.cycleWorld() }

        assertEquals(listOf(0, 1, 2), events)
    }

    @Test
    fun `player and npc entrypoints exist`() {
        val methodNames = ContentScheduling::class.java.methods.map { it.name }.toSet()

        assertTrue("player" in methodNames)
        assertTrue("npc" in methodNames)
    }
}
