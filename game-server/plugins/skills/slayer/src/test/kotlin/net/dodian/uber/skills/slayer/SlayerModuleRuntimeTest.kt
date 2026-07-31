package net.dodian.uber.skills.slayer

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.api.plugin.skills.SkillEquipmentSlot
import net.dodian.uber.game.api.plugin.skills.SkillNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillNpcRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private fun npcRef(id: Int) = SkillNpcRef(id, index = 0, position = SkillPosition(3200, 3200, 0))

class SlayerModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(SlayerModule.descriptor.id, Skill.SLAYER)
        assertEquals(SlayerModule.descriptor.id, SlayerModule.contentManifest.id)
    }

    @Test
    fun `complete task registry preserves legacy ordinal ordering`() {
        assertEquals(34, SlayerModule.tasks.size)
        assertEquals("Crawling Hands", SlayerModule.tasks.first().name)
        assertEquals("Abyssal guardian", SlayerModule.tasks.last().name)
        assertEquals("Abyssal Demons", SlayerTaskRegistry.forOrdinal(20)?.name)
    }

    @Test
    fun `combat rules enforce active task and imbued mask bonus`() {
        val player = FakeSkillPlayer().apply {
            slayerTaskOrdinal = 20
            slayerAssignmentAmount = 30
            slayerRemainingAmount = 1
            setLevel(Skill.SLAYER, 99)
            equip(SkillEquipmentSlot.HEAD, 11784)
        }
        assertEquals(null, SlayerCombatService.canAttack(player, 415, false))
        assertEquals(2, SlayerCombatService.damageBonus(player, 415, true))
        assertEquals(2, SlayerCombatService.damageBonus(player, 415, false))
    }

    @Test
    fun `death completion applies legacy tenth-task bonus`() {
        val player = FakeSkillPlayer().apply {
            slayerTaskOrdinal = 20
            slayerAssignmentAmount = 30
            slayerRemainingAmount = 1
            slayerStreak = 9
        }
        val outcome = SlayerDeathService.onNpcDeath(player, 415, 100)!!
        assertTrue(outcome.completion)
        assertEquals(10, outcome.streak)
        assertEquals(1_100, outcome.baseXp)
        assertEquals(6_000, outcome.bonusXp)
    }

    // --- Slayer master conversations ------------------------------------------------------
    // random.between(min,max) is deterministic in FakeSkillPlayer (always returns min), so the
    // candidate-task pick and the amount roll are both predictable: for master 402 (Mazchna)
    // with slayer level 10, only ordinals 0 (Crawling Hands, 1-40), 5 (Hill Giants, 1-50) and
    // 8 (Fire Giants, 1-200) survive the level filter, in that candidate-array order - so the
    // task always comes out as ordinal 0 / amount 20 (its amountMin).
    //
    // The task-assign/cancel/upgrade branches report their result via ui.message (checked
    // against player.messages), not an npcChat dialogue line - see the finishThen comment on
    // the "I'd like a task please" branch in SlayerModule.kt for why (the branch-builder DSL
    // evaluates every option eagerly at menu-construction time, so a direct side-effecting
    // call there would fire just from opening the menu; deferring via finishThen fixes it, at
    // the cost of downgrading that one reply from an npc-chat bubble to a plain message -
    // matching the same downgrade already applied to other migrated skills' NPC chat text).

    @Test
    fun `talkto and assignment options are registered for all three slayer masters`() {
        listOf(402, 403, 405).forEach { npcId ->
            assertTrue(SlayerModule.definition.npcBindings.any { it.option == 1 && npcId in it.npcIds })
            assertTrue(SlayerModule.definition.npcBindings.any { it.option == 3 && npcId in it.npcIds })
        }
    }

    @Test
    fun `talking to a slayer master and requesting a task assigns the first eligible candidate`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.SLAYER, 10) }
        val talkTo = SlayerModule.definition.npcBindings.single { it.option == 1 && 402 in it.npcIds }

        assertTrue(talkTo.handler(SkillNpcInteraction(player, 1, npcRef(402))))
        assertEquals(listOf("Need help with your slayer assignment?"), player.dialogueNpcChatLines)
        assertEquals(listOf("Yes please.", "No thanks."), player.pendingDialogueOptionTexts())

        assertTrue(player.selectDialogueOption("Yes please."))
        assertEquals(
            listOf(
                "I'd like a task please",
                "No task to skip",
                "I'd like to upgrade my black mask",
                "Can you teleport me to west ardougne?",
            ),
            player.pendingDialogueOptionTexts(),
        )

        assertTrue(player.selectDialogueOption("I'd like a task please"))
        assertEquals(402, player.slayerTask.masterNpcId)
        assertEquals(0, player.slayerTask.ordinal)
        assertEquals(20, player.slayerTask.remainingAmount)
    }

    @Test
    fun `duradel requires 50 combat and slayer level`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.SLAYER, 10) } // combatLevelValue defaults to 3
        val talkTo = SlayerModule.definition.npcBindings.single { it.option == 1 && 405 in it.npcIds }

        talkTo.handler(SkillNpcInteraction(player, 1, npcRef(405)))
        assertTrue(player.selectDialogueOption("Yes please."))

        assertNull(player.pendingDialogueOptionTexts())
        assertTrue(player.dialogueNpcChatLines.contains("You need 50 combat and slayer"))
        assertEquals(-1, player.slayerTask.ordinal)
    }

    @Test
    fun `vannaka requires a crystal key and 50 slayer`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.SLAYER, 99) }
        val talkTo = SlayerModule.definition.npcBindings.single { it.option == 1 && 403 in it.npcIds }

        talkTo.handler(SkillNpcInteraction(player, 1, npcRef(403)))
        assertTrue(player.selectDialogueOption("Yes please."))

        assertNull(player.pendingDialogueOptionTexts())
        assertTrue(player.dialogueNpcChatLines.contains("You need a crystal key and 50 slayer"))
        assertEquals(-1, player.slayerTask.ordinal)
    }

    @Test
    fun `direct assignment option assigns a task without the menu`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.SLAYER, 10) }
        val assign = SlayerModule.definition.npcBindings.single { it.option == 3 && 402 in it.npcIds }

        assertTrue(assign.handler(SkillNpcInteraction(player, 3, npcRef(402))))

        assertEquals(0, player.slayerTask.ordinal)
        assertEquals(20, player.slayerTask.remainingAmount)
        assertEquals(
            listOf(
                "You must go out and kill 20 Crawling Hands",
                "If you want a new task that's too bad",
                "Visit Dodian.net for a slayer guide",
            ),
            player.dialogueNpcChatLines,
        )
    }

    @Test
    fun `direct assignment refuses when a task is already active`() {
        val player = FakeSkillPlayer().apply {
            setLevel(Skill.SLAYER, 10)
            slayerMasterNpcId = 402
            slayerTaskOrdinal = 0
            slayerRemainingAmount = 5
        }
        val assign = SlayerModule.definition.npcBindings.single { it.option == 3 && 402 in it.npcIds }

        assign.handler(SkillNpcInteraction(player, 3, npcRef(402)))

        assertEquals(listOf("You already have a task!"), player.dialogueNpcChatLines)
        assertEquals(5, player.slayerTask.remainingAmount)
    }

    @Test
    fun `cancel flow with sufficient coins cancels the task and deducts coins`() {
        val player = FakeSkillPlayer(mapOf(995 to 200_000)).apply {
            setLevel(Skill.SLAYER, 10)
            slayerMasterNpcId = 402
            slayerTaskOrdinal = 0
            slayerRemainingAmount = 5
        }
        val talkTo = SlayerModule.definition.npcBindings.single { it.option == 1 && 402 in it.npcIds }

        talkTo.handler(SkillNpcInteraction(player, 1, npcRef(402)))
        player.selectDialogueOption("Yes please.")
        assertTrue(player.selectDialogueOption("Cancel crawling hands task"))
        assertEquals(listOf("Yes", "No"), player.pendingDialogueOptionTexts())

        assertTrue(player.selectDialogueOption("Yes"))

        assertEquals(0, player.slayerTask.remainingAmount)
        assertEquals(100_000, player.amount(995)) // Mazchna cancel cost is 100,000
        assertTrue(player.messages.contains("I have now canceled your crawling hands task!"))
    }

    @Test
    fun `cancel flow with insufficient coins shows a message and does not cancel`() {
        val player = FakeSkillPlayer(mapOf(995 to 100)).apply {
            setLevel(Skill.SLAYER, 10)
            slayerMasterNpcId = 402
            slayerTaskOrdinal = 0
            slayerRemainingAmount = 5
        }
        val talkTo = SlayerModule.definition.npcBindings.single { it.option == 1 && 402 in it.npcIds }

        talkTo.handler(SkillNpcInteraction(player, 1, npcRef(402)))
        player.selectDialogueOption("Yes please.")
        player.selectDialogueOption("Cancel crawling hands task")
        player.selectDialogueOption("Yes")

        assertTrue(player.messages.contains("You do not have enough coins to cancel your task!"))
        assertEquals(5, player.slayerTask.remainingAmount)
        assertEquals(100, player.amount(995))
    }

    @Test
    fun `cancel flow with no active task shows a message`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.SLAYER, 10) }
        val talkTo = SlayerModule.definition.npcBindings.single { it.option == 1 && 402 in it.npcIds }

        talkTo.handler(SkillNpcInteraction(player, 1, npcRef(402)))
        player.selectDialogueOption("Yes please.")
        assertTrue(player.selectDialogueOption("No task to skip"))

        assertTrue(player.dialogueNpcChatLines.contains("You do not have a task currently."))
    }

    @Test
    fun `upgrade flow with a black mask and enough coins upgrades to an imbued mask`() {
        val player = FakeSkillPlayer(mapOf(8921 to 1, 995 to 2_000_000)).apply { setLevel(Skill.SLAYER, 10) }
        val talkTo = SlayerModule.definition.npcBindings.single { it.option == 1 && 402 in it.npcIds }

        talkTo.handler(SkillNpcInteraction(player, 1, npcRef(402)))
        player.selectDialogueOption("Yes please.")
        assertTrue(player.selectDialogueOption("I'd like to upgrade my black mask"))
        assertEquals(listOf("Yes, please", "No, please"), player.pendingDialogueOptionTexts())

        assertTrue(player.selectDialogueOption("Yes, please"))

        assertEquals(0, player.amount(8921))
        assertEquals(1, player.amount(11784))
        assertEquals(0, player.amount(995))
        assertTrue(player.messages.contains("Here is your imbued black mask."))
    }

    @Test
    fun `upgrade flow without a mask or helmet shows a message`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.SLAYER, 10) }
        val talkTo = SlayerModule.definition.npcBindings.single { it.option == 1 && 402 in it.npcIds }

        talkTo.handler(SkillNpcInteraction(player, 1, npcRef(402)))
        player.selectDialogueOption("Yes please.")
        assertTrue(player.selectDialogueOption("I'd like to upgrade my black mask"))

        assertNull(player.pendingDialogueOptionTexts())
        assertTrue(player.dialogueNpcChatLines.contains("You do not have a black mask or a slayer helmet!"))
    }

    @Test
    fun `upgrade flow with insufficient coins shows a message and does not upgrade`() {
        val player = FakeSkillPlayer(mapOf(8921 to 1)).apply { setLevel(Skill.SLAYER, 10) }
        val talkTo = SlayerModule.definition.npcBindings.single { it.option == 1 && 402 in it.npcIds }

        talkTo.handler(SkillNpcInteraction(player, 1, npcRef(402)))
        player.selectDialogueOption("Yes please.")
        player.selectDialogueOption("I'd like to upgrade my black mask")
        player.selectDialogueOption("Yes, please")

        assertTrue(player.messages.contains("You do not have enough money!"))
        assertEquals(1, player.amount(8921))
    }

    @Test
    fun `teleport option teleports to west ardougne`() {
        val player = FakeSkillPlayer().apply { setLevel(Skill.SLAYER, 10) }
        val talkTo = SlayerModule.definition.npcBindings.single { it.option == 1 && 402 in it.npcIds }

        talkTo.handler(SkillNpcInteraction(player, 1, npcRef(402)))
        player.selectDialogueOption("Yes please.")
        assertTrue(player.selectDialogueOption("Can you teleport me to west ardougne?"))

        assertEquals(SkillPosition(2542, 3306, 0), player.world.position)
        assertTrue(player.dialogueNpcChatLines.contains("Be careful out there!"))
    }
}
