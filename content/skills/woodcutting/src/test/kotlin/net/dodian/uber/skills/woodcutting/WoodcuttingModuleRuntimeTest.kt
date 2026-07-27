package net.dodian.uber.skills.woodcutting

import net.dodian.uber.game.api.plugin.skills.SkillEquipmentSlot
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectRef
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val NORMAL_TREE_OBJECT_ID = 1276
private const val BRONZE_AXE = 1351
private const val RUNE_AXE = 1359
private const val LOG_ITEM_ID = 1511

private fun treeRef(id: Int = NORMAL_TREE_OBJECT_ID) = SkillObjectRef(id, SkillPosition(3200, 3200, 0))

class WoodcuttingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(WoodcuttingModule.descriptor.id, Skill.WOODCUTTING)
        assertEquals(WoodcuttingModule.descriptor.id, WoodcuttingModule.contentManifest.id)
    }

    @Test
    fun `data loads all 6 tiers and 9 axes`() {
        assertEquals(6, WoodcuttingModule.trees.size)
        assertEquals(9, WoodcuttingModule.axes.size)
        assertTrue(WoodcuttingModule.trees.first { it.name == "normal" }.objectIds.contains(NORMAL_TREE_OBJECT_ID))
    }

    @Test
    fun `object click cuts a tree and eventually yields a log`() {
        val binding = WoodcuttingModule.definition.objectBindings.single { NORMAL_TREE_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(BRONZE_AXE to 1)).apply { setLevel(Skill.WOODCUTTING, 1) }

        val handled = binding.handler(SkillObjectInteraction(player, 1, treeRef()))
        assertTrue(handled)
        assertEquals(listOf("You swing your axe at the tree..."), player.messages)

        player.advanceTicks(6)

        assertTrue(player.amount(LOG_ITEM_ID) >= 1, "expected at least one log cut")
        assertTrue(player.skills.experience(Skill.WOODCUTTING) >= 100)
        assertEquals(LOG_ITEM_ID, player.gatheringLogs.first().first)
        assertEquals("Woodcutting", player.gatheringLogs.first().third)
    }

    @Test
    fun `no axe refuses to start`() {
        val binding = WoodcuttingModule.definition.objectBindings.single { NORMAL_TREE_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer()

        binding.handler(SkillObjectInteraction(player, 1, treeRef()))

        assertEquals(listOf("You need an axe in which you got the required woodcutting level for."), player.messages)
        assertNull(player.activeActionName())
    }

    @Test
    fun `equipped axe in the weapon slot counts as owning it`() {
        val binding = WoodcuttingModule.definition.objectBindings.single { NORMAL_TREE_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer().apply {
            setLevel(Skill.WOODCUTTING, 1)
            equip(SkillEquipmentSlot.WEAPON, BRONZE_AXE)
        }

        val handled = binding.handler(SkillObjectInteraction(player, 1, treeRef()))

        assertTrue(handled)
        assertEquals(listOf("You swing your axe at the tree..."), player.messages)
    }

    @Test
    fun `insufficient level for the tree refuses to start`() {
        val magicTree = WoodcuttingModule.trees.single { it.name == "magic" }
        val binding = WoodcuttingModule.definition.objectBindings.single { magicTree.objectIds.first() in it.objectIds }
        val player = FakeSkillPlayer(mapOf(BRONZE_AXE to 1)).apply { setLevel(Skill.WOODCUTTING, 1) }

        binding.handler(SkillObjectInteraction(player, 1, treeRef(magicTree.objectIds.first())))

        assertEquals(
            listOf("You need a woodcutting level of ${magicTree.requiredLevel} to cut this tree."),
            player.messages,
        )
        assertNull(player.activeActionName())
    }

    @Test
    fun `walking away from the tree stops the action`() {
        val binding = WoodcuttingModule.definition.objectBindings.single { NORMAL_TREE_OBJECT_ID in it.objectIds }
        val player = FakeSkillPlayer(mapOf(BRONZE_AXE to 1)).apply { setLevel(Skill.WOODCUTTING, 99) }

        binding.handler(SkillObjectInteraction(player, 1, treeRef()))
        assertEquals("woodcutting", player.activeActionName())

        player.withinBoundaryOverride = false
        player.advanceTicks(1)

        assertEquals("You moved too far away.", player.messages.last())
        assertNull(player.activeActionName())
    }

    @Test
    fun `resolveBestAxe prefers the highest tier the player has`() {
        val player = FakeSkillPlayer(mapOf(BRONZE_AXE to 1, RUNE_AXE to 1)).apply { setLevel(Skill.WOODCUTTING, 99) }
        val best = WoodcuttingModule.resolveBestAxe(player)
        assertEquals(RUNE_AXE, best?.itemId)
    }

    @Test
    fun `stopAction halts an in-progress woodcutting trip`() {
        val player = FakeSkillPlayer(mapOf(BRONZE_AXE to 1)).apply { setLevel(Skill.WOODCUTTING, 1) }
        val binding = WoodcuttingModule.definition.objectBindings.single { NORMAL_TREE_OBJECT_ID in it.objectIds }

        binding.handler(SkillObjectInteraction(player, 1, treeRef()))
        assertEquals("woodcutting", player.activeActionName())

        WoodcuttingModule.stopAction(player)
        assertNull(player.activeActionName())

        val logsBefore = player.amount(LOG_ITEM_ID)
        player.advanceTicks(10)
        assertEquals(logsBefore, player.amount(LOG_ITEM_ID))
    }
}
