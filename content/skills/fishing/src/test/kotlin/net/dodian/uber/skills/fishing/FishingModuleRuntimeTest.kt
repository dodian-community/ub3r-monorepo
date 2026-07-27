package net.dodian.uber.skills.fishing

import net.dodian.uber.game.api.plugin.skills.SkillNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillNpcRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val NET_SPOT_NPC_ID = 1510
private const val SHRIMP = 317
private const val SMALL_NET = 303

private fun npcRef(id: Int) = SkillNpcRef(id, index = 0, position = SkillPosition(3200, 3200, 0))

class FishingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(FishingModule.descriptor.id, Skill.FISHING)
        assertEquals(FishingModule.descriptor.id, FishingModule.contentManifest.id)
    }

    @Test
    fun `npc click starts fishing and eventually catches a fish`() {
        val binding = FishingModule.definition.npcBindings.single { NET_SPOT_NPC_ID in it.npcIds && it.option == 1 }
        val player = FakeSkillPlayer(mapOf(SMALL_NET to 1)).apply { setLevel(Skill.FISHING, 99) }

        val handled = binding.handler(SkillNpcInteraction(player, 1, npcRef(NET_SPOT_NPC_ID)))
        assertTrue(handled)
        assertEquals(listOf("You start fishing..."), player.messages)

        player.advanceTicks(5)

        assertTrue(player.amount(SHRIMP) >= 1, "expected at least one shrimp caught")
        assertTrue(player.skills.experience(Skill.FISHING) >= 110)
        assertEquals(SHRIMP, player.gatheringLogs.first().first)
        assertEquals("Fishing", player.gatheringLogs.first().third)
    }

    @Test
    fun `missing bait or tool refuses to start`() {
        val binding = FishingModule.definition.npcBindings.single { NET_SPOT_NPC_ID in it.npcIds && it.option == 1 }
        val player = FakeSkillPlayer()

        binding.handler(SkillNpcInteraction(player, 1, npcRef(NET_SPOT_NPC_ID)))

        assertEquals(listOf("You need a item $SMALL_NET to fish here."), player.messages)
        assertNull(player.activeActionName())
    }

    @Test
    fun `harpoon in inventory substitutes for the required tool at high level`() {
        val binding = FishingModule.definition.npcBindings.single { NET_SPOT_NPC_ID in it.npcIds && it.option == 1 }
        val player = FakeSkillPlayer(mapOf(21028 to 1)).apply { setLevel(Skill.FISHING, 99) }

        val handled = binding.handler(SkillNpcInteraction(player, 1, npcRef(NET_SPOT_NPC_ID)))

        assertTrue(handled)
        assertEquals(listOf("You start fishing..."), player.messages)
    }

    @Test
    fun `feather-consuming spot requires and consumes feathers`() {
        val binding = FishingModule.definition.npcBindings.single { NET_SPOT_NPC_ID in it.npcIds && it.option == 2 }
        val player = FakeSkillPlayer(mapOf(309 to 1, 314 to 1)).apply { setLevel(Skill.FISHING, 20) }

        binding.handler(SkillNpcInteraction(player, 2, npcRef(NET_SPOT_NPC_ID)))
        player.advanceTicks(6)

        assertEquals(0, player.amount(314))
        assertTrue(player.amount(335) >= 1)
    }

    @Test
    fun `stopAction halts an in-progress fishing trip before any catch`() {
        val player = FakeSkillPlayer(mapOf(SMALL_NET to 1)).apply { setLevel(Skill.FISHING, 99) }

        FishingModule.attempt(player, NET_SPOT_NPC_ID, 1)
        assertEquals("fishing", player.activeActionName())

        FishingModule.stopAction(player)
        assertNull(player.activeActionName())

        player.advanceTicks(10)
        assertEquals(0, player.amount(SHRIMP))
    }

    @Test
    fun `resting after four catches stops the action`() {
        // FakeSkillPlayer's random always returns the minimum, so chance(1, 20) is always true here.
        val player = FakeSkillPlayer(mapOf(SMALL_NET to 1)).apply { setLevel(Skill.FISHING, 99) }

        FishingModule.attempt(player, NET_SPOT_NPC_ID, 1)
        player.advanceTicks(20)

        assertEquals(4, player.amount(SHRIMP))
        assertTrue(player.messages.any { it.startsWith("You take a rest after gathering 4 resources") })
        assertNull(player.activeActionName())
    }

    @Test
    fun `premium only spot refuses non-premium players`() {
        val binding = FishingModule.definition.npcBindings.single { 1514 in it.npcIds && it.option == 1 }
        val player = FakeSkillPlayer(mapOf(303 to 1)).apply { setLevel(Skill.FISHING, 99) }

        binding.handler(SkillNpcInteraction(player, 1, npcRef(1514)))

        assertEquals(listOf("You need to be premium to fish from this spot!"), player.messages)
        assertNull(player.activeActionName())
    }
}
