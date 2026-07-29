package net.dodian.uber.game.model.entity.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BossKillLogStateTest {
    @Test
    fun `npc names update the matching persisted boss counter`() {
        val state = BossKillLogState()

        state.incrementForNpcName("King Black Dragon")
        state.incrementForNpcName("King Black Dragon")

        assertEquals(2, state.countForNpcName("King Black Dragon"))
        state.set("King_Black_Dragon", BossKillLogState.MAX_DISPLAYED_COUNT)
        state.incrementForNpcName("King Black Dragon")
        assertEquals(BossKillLogState.MAX_DISPLAYED_COUNT, state.countForNpcName("King Black Dragon"))
    }
}
