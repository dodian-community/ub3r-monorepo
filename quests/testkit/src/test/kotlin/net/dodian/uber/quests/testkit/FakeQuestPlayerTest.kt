package net.dodian.uber.quests.testkit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FakeQuestPlayerTest {
    @Test
    fun `stage tracking is independent per quest key`() {
        val player = FakeQuestPlayer()
        assertEquals(0, player.questProgress.stage("quest_a"))
        player.questProgress.advance("quest_a")
        player.questProgress.advance("quest_a")
        assertEquals(2, player.questProgress.stage("quest_a"))
        assertEquals(0, player.questProgress.stage("quest_b"))
    }

    @Test
    fun `attribute handle reads and resets independently of stage`() {
        val player = FakeQuestPlayer()
        val given = player.questProgress.attribute("quest_a", "given_item", false)
        assertEquals(false, given.get())
        given.set(true)
        assertEquals(true, given.get())
        assertEquals(false, given.reset())
    }

    @Test
    fun `delegates inventory and ui calls to the underlying fake skill player`() {
        val player = FakeQuestPlayer(initialItems = mapOf(1291 to 1))
        assertTrue(player.inventory.contains(1291))
        player.ui.message("hello")
        assertEquals(listOf("hello"), player.fake.messages)
    }
}
