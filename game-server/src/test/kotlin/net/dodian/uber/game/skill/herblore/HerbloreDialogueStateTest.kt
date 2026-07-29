package net.dodian.uber.game.skill.herblore

import net.dodian.uber.game.activity.partyroom.PartyRoomRewardItem
import kotlin.test.Test
import kotlin.test.assertEquals

class HerbloreDialogueStateTest {
    @Test
    fun `clear resets the active menu and its transient options`() {
        val state = HerbloreDialogueState()
        state.page = 3
        state.options += PartyRoomRewardItem(199, 1)

        state.clear()

        assertEquals(-1, state.page)
        assertEquals(emptyList(), state.options)
    }
}
