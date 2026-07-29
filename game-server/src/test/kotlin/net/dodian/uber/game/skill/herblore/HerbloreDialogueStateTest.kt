package net.dodian.uber.game.skill.herblore

import net.dodian.uber.game.activity.partyroom.PartyRoomRewardItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HerbloreDialogueStateTest {
    @Test
    fun `clear resets the active menu and its transient options`() {
        val state = HerbloreDialogueState()
        state.page = 3
        state.options += PartyRoomRewardItem(199, 1)

        state.clear()

        assertEquals(-1, state.page)
        assertTrue(state.options.isEmpty())
    }
}
