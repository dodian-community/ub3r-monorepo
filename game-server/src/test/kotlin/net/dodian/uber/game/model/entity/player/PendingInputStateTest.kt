package net.dodian.uber.game.model.entity.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PendingInputStateTest {
    @Test
    fun `new input request replaces the previous consumer and clear resets it`() {
        val state = PlayerContentRuntimeState()

        state.setPendingInputState(PendingInputState.BANK_SEARCH)
        state.setPendingInputState(PendingInputState.ADD_FRIEND)
        assertEquals(PendingInputState.ADD_FRIEND, state.getPendingInputState())

        state.clearPendingInputState()
        assertEquals(PendingInputState.NONE, state.getPendingInputState())
    }
}
