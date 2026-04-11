package net.dodian.uber.game.engine.systems.action

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DuelCountdownServiceTest {
    @Test
    fun `countdown emits 3 2 1 then fight`() {
        var state = DuelCountdownState.initial()
        val chats = mutableListOf<String>()
        var canAttack = false

        while (!state.done) {
            val step = DuelCountdownService.advance(state)
            step.forceChat?.let { chats += it }
            canAttack = canAttack || step.enableCombat
            state = step.nextState
        }

        assertEquals(listOf("3", "2", "1", "Fight!"), chats)
        assertTrue(canAttack)
    }
}
