package net.dodian.uber.game.model.entity.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RunePouchStateTest {
    @Test
    fun `pouch contents serialize in the existing persistence format`() {
        val state = RunePouchState()

        state.setAmount(0, 4)
        state.setAmount(1, 7)
        state.addAmount(2, 3)

        assertEquals(1, state.levelRequirement(0))
        assertEquals(13, state.capacity(3))
        assertEquals("4:7:3:0", state.saveValue())
    }
}
