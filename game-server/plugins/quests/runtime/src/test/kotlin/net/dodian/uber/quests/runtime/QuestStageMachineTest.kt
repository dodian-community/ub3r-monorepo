package net.dodian.uber.quests.runtime

import net.dodian.uber.quests.testkit.FakeQuestPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QuestStageMachineTest {
    @Test
    fun `dispatch runs the handler for the given stage`() {
        val visited = mutableListOf<Pair<Int, Boolean>>()
        val machine = QuestStageMachine(
            mapOf(
                0 to QuestStageHandler { _, stage, resuming -> visited += stage to resuming },
                1 to QuestStageHandler { _, stage, resuming -> visited += stage to resuming },
            ),
        )
        val player = FakeQuestPlayer()

        machine.dispatch(player, 1, resuming = false)

        assertEquals(listOf(1 to false), visited)
    }

    @Test
    fun `dispatch marks resuming on login-resume calls`() {
        val visited = mutableListOf<Pair<Int, Boolean>>()
        val machine = QuestStageMachine(mapOf(2 to QuestStageHandler { _, stage, resuming -> visited += stage to resuming }))
        val player = FakeQuestPlayer()

        machine.dispatch(player, 2, resuming = true)

        assertEquals(listOf(2 to true), visited)
    }

    @Test
    fun `unhandled stage numbers are a no-op`() {
        val machine = QuestStageMachine(emptyMap())
        val player = FakeQuestPlayer()

        machine.dispatch(player, 5)
    }
}
