package net.dodian.uber.quests.tutorialisland

import net.dodian.uber.game.api.plugin.quests.QuestNpcInteraction
import net.dodian.uber.game.api.plugin.quests.QuestObjectInteraction
import net.dodian.uber.game.api.plugin.quests.QuestState
import net.dodian.uber.game.api.plugin.skills.SkillNpcRef
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.quests.testkit.FakeQuestPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TutorialIslandModuleTest {
    private val definition = TutorialIslandModule.definition
    private val guideHandler = definition.npcBindings.first { 9476 in it.npcIds }.handler
    private val survivalExpertHandler = definition.npcBindings.first { 9477 in it.npcIds }.handler
    private val doorHandler = definition.objectBindings.single().handler

    private fun talkToGuide(player: FakeQuestPlayer) =
        guideHandler(QuestNpcInteraction(player, 1, SkillNpcRef(9476, 0, SkillPosition(0, 0, 0))))

    private fun openDoor(player: FakeQuestPlayer) =
        doorHandler(QuestObjectInteraction(player, 1, SkillObjectRef(9398, SkillPosition(0, 0, 0))))

    private fun talkToSurvivalExpert(player: FakeQuestPlayer) =
        survivalExpertHandler(QuestNpcInteraction(player, 1, SkillNpcRef(9477, 0, SkillPosition(0, 0, 0))))

    @Test
    fun `walking the full sequence completes the quest and grants starter gear`() {
        val player = FakeQuestPlayer()

        assertEquals(QuestState.NOT_STARTED, TutorialIslandModule.quest.questState(player))

        talkToGuide(player)
        assertEquals(1, TutorialIslandModule.quest.getQuestStage(player))

        openDoor(player)
        assertEquals(2, TutorialIslandModule.quest.getQuestStage(player))

        talkToSurvivalExpert(player)
        assertEquals(QuestState.FINISHED, TutorialIslandModule.quest.questState(player))
        assertTrue(player.inventory.contains(1351)) // bronze axe
        assertTrue(player.inventory.contains(590)) // tinderbox
    }

    @Test
    fun `the door click never blocks the object's own default behavior`() {
        val player = FakeQuestPlayer()
        talkToGuide(player)

        val handled = openDoor(player)

        assertFalse(handled)
    }

    @Test
    fun `opening the door before talking to the guide does not advance the stage`() {
        val player = FakeQuestPlayer()

        openDoor(player)

        assertEquals(0, TutorialIslandModule.quest.getQuestStage(player))
    }

    @Test
    fun `talking to the survival expert before reaching her stage does not grant items`() {
        val player = FakeQuestPlayer()
        talkToGuide(player)

        talkToSurvivalExpert(player)

        assertEquals(1, TutorialIslandModule.quest.getQuestStage(player))
        assertFalse(player.inventory.contains(1351))
    }

    @Test
    fun `talking to the guide after finishing is a no-op`() {
        val player = FakeQuestPlayer()
        talkToGuide(player)
        openDoor(player)
        talkToSurvivalExpert(player)

        talkToGuide(player)

        assertEquals(TutorialIslandModule.quest.maxSteps, TutorialIslandModule.quest.getQuestStage(player))
    }

    @Test
    fun `resume re-dispatches the current stage without advancing it`() {
        val player = FakeQuestPlayer()
        talkToGuide(player)

        TutorialIslandModule.resume(player)

        assertEquals(1, TutorialIslandModule.quest.getQuestStage(player))
    }

    @Test
    fun `startOrResume starts a fresh player and resumes an in-progress one`() {
        val player = FakeQuestPlayer()

        TutorialIslandModule.startOrResume(player)
        assertEquals(1, TutorialIslandModule.quest.getQuestStage(player))

        TutorialIslandModule.startOrResume(player)
        assertEquals(1, TutorialIslandModule.quest.getQuestStage(player))
    }

    @Test
    fun `forceAdvance skips a stage without the real interaction`() {
        val player = FakeQuestPlayer()
        talkToGuide(player)

        val stage = TutorialIslandModule.forceAdvance(player)

        assertEquals(2, stage)
        assertEquals(2, TutorialIslandModule.quest.getQuestStage(player))
    }
}
