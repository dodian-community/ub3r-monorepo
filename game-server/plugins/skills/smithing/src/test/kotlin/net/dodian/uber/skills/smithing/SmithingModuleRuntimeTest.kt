package net.dodian.uber.skills.smithing

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SmithingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(SmithingModule.descriptor.id, Skill.SMITHING)
        assertEquals(SmithingModule.descriptor.id, SmithingModule.contentManifest.id)
    }

    @Test
    fun `toml smelting recipes are loaded`() {
        assertTrue(SmithingModule.smeltingRecipes.isNotEmpty())
        assertTrue(SmithingModule.smeltingRecipes.any { it.barId == 2349 })
    }

    @Test
    fun `plugin smelting registry preserves legacy iron chance and furnace mappings`() {
        val iron = requireNotNull(SmeltingRegistry.findRecipe(2351))
        assertEquals(54, iron.successChancePercent)
        assertEquals("You fail to refine the iron", iron.failureMessage)
        assertEquals(8, SmeltingRegistry.recipes.size)
        assertEquals(FurnaceButtonMapping(15147, 2349, 1), SmeltingRegistry.mapping(15147))
        assertEquals(FurnaceButtonMapping(2414, 2349, 0), SmeltingRegistry.mapping(2414))
        assertTrue(SmeltingRegistry.isFurnaceFrame(2405))
        assertTrue(SmeltingRegistry.isFurnaceButton(2414))
    }

    @Test
    fun `furnace routes use only verified current cache furnaces`() {
        assertEquals(setOf(2030, 3994, 16469), SmithingModule.furnaceObjectIds.toSet())
        assertFalse(SmithingModule.furnaceObjectIds.any { it in setOf(2150, 2151, 2152, 2153, 11666, 29662) })
        SmithingModule.furnaceObjectIds.forEach { objectId ->
            assertTrue(SmithingModule.definition.itemOnObjectBindings.any { objectId in it.objectIds })
        }
        setOf(2030, 16469).forEach { objectId ->
            assertTrue(SmithingModule.definition.objectBindings.any { it.option == 2 && objectId in it.objectIds })
        }
        assertFalse(SmithingModule.definition.objectBindings.any { it.option == 1 && it.objectIds.any { id -> id in SmithingModule.furnaceObjectIds } })
        assertFalse(SmithingModule.definition.objectBindings.any { 3994 in it.objectIds })
    }

    @Test
    fun `anvil routes exclude revision 218 table id`() {
        assertEquals(setOf(2097), SmithingModule.anvilObjectIds.toSet())
        assertFalse(SmithingModule.definition.itemOnObjectBindings.any { 2783 in it.objectIds })
    }

    @Test
    fun `plugin owns repeated furnace action and clears it on completion`() {
        val player = net.dodian.uber.skills.testkit.FakeSkillPlayer(
            mapOf(436 to 2, 438 to 2),
        ).apply { setLevel(Skill.SMITHING, 99) }

        assertTrue(SmithingSmeltingService.start(player, 2349, 2))
        assertEquals("smithing.smelt", player.activeActionName())
        player.advanceTicks(4)

        assertEquals(0, player.amount(436))
        assertEquals(0, player.amount(438))
        assertEquals(2, player.amount(2349))
        assertEquals(2 * requireNotNull(SmeltingRegistry.findRecipe(2349)).experience, player.skills.experience(Skill.SMITHING))
        assertEquals(2, player.randomEventChecks)
        assertEquals(null, player.activeActionName())
    }

    @Test
    fun `furnace action preserves legacy missing ore message and cancellation`() {
        val missingCoal = net.dodian.uber.skills.testkit.FakeSkillPlayer(
            mapOf(440 to 1, 453 to 1),
        ).apply { setLevel(Skill.SMITHING, 99) }

        assertTrue(SmithingSmeltingService.start(missingCoal, 2353, 1))
        missingCoal.advanceTicks()
        assertEquals("You need a iron ore and 2 coal to do this!", missingCoal.messages.single())
        assertEquals(1, missingCoal.amount(440))
        assertEquals(1, missingCoal.amount(453))
        assertEquals(0, missingCoal.amount(2353))

        val cancelled = net.dodian.uber.skills.testkit.FakeSkillPlayer(
            mapOf(436 to 1, 438 to 1),
        ).apply { setLevel(Skill.SMITHING, 99) }
        assertTrue(SmithingSmeltingService.start(cancelled, 2349, 1))
        SmithingSmeltingService.stopAction(cancelled)
        cancelled.advanceTicks(4)
        assertEquals(1, cancelled.amount(436))
        assertEquals(1, cancelled.amount(438))
        assertEquals(0, cancelled.amount(2349))
    }

    @Test
    fun `pending furnace selection is plugin runtime state`() {
        val player = net.dodian.uber.skills.testkit.FakeSkillPlayer()

        assertTrue(SmithingSmeltingService.selectPending(player, 2351))
        assertEquals(2351, SmithingSmeltingService.pendingBar(player))
        SmithingSmeltingService.clearPending(player)
        assertEquals(-1, SmithingSmeltingService.pendingBar(player))
        assertFalse(SmithingSmeltingService.selectPending(player, 9999))
    }

    @Test
    fun `plugin owns anvil registry interface and transient selection`() {
        val player = net.dodian.uber.skills.testkit.FakeSkillPlayer(
            mapOf(2347 to 1, 2349 to 5),
        ).apply { setLevel(Skill.SMITHING, 99) }

        assertTrue(SmithingAnvilService.openForBar(player, 2349, 3201, 3202))
        assertEquals(listOf(994), player.openedInterfaces)
        assertEquals(setOf(1119, 1120, 1121, 1122, 1123), player.itemGrids.keys)
        assertEquals(1, SmithingAnvilService.activeSelection(player)?.tierId)
        assertEquals(3201, SmithingAnvilService.activeSelection(player)?.anvilX)

        val request = SmithingAnvilService.resolveRequest(
            player = player,
            interfaceId = 1119,
            itemId = 1205,
            slot = 0,
            amount = 3,
            fallbackAnchorX = -1,
            fallbackAnchorY = -1,
        )
        assertEquals(1205, request?.product?.itemId)
        assertEquals(3, request?.amount)
        assertEquals(2349, request?.barId)

        SmithingAnvilService.clear(player)
        assertEquals(null, SmithingAnvilService.activeSelection(player))
    }

    @Test
    fun `anvil interface rejects missing hammer without creating state`() {
        val player = net.dodian.uber.skills.testkit.FakeSkillPlayer(mapOf(2349 to 1))

        assertFalse(SmithingAnvilService.openForBar(player, 2349, 3201, 3202))
        assertEquals("You need a item 2347 to hammer bars.", player.messages.single())
        assertEquals(null, SmithingAnvilService.activeSelection(player))
        assertTrue(player.openedInterfaces.isEmpty())
    }

    @Test
    fun `plugin owns delayed repeated anvil action and cleanup`() {
        val player = net.dodian.uber.skills.testkit.FakeSkillPlayer(
            mapOf(2347 to 1, 2349 to 2),
        ).apply {
            setLevel(Skill.SMITHING, 99)
            positionValue = net.dodian.uber.game.api.plugin.skills.SkillPosition(3201, 3202)
        }
        assertTrue(SmithingAnvilService.openForBar(player, 2349, 3201, 3202))
        val request = requireNotNull(
            SmithingAnvilService.resolveRequest(
                player, 1119, 1205, 0, 2, -1, -1,
            ),
        )

        assertTrue(SmithingAnvilService.start(player, request))
        assertEquals("smithing.anvil", player.activeActionName())
        player.advanceTicks(3)
        assertEquals(0, player.amount(1205), "the initial hammer delay must be preserved")
        player.advanceTicks(4)

        assertEquals(0, player.amount(2349))
        assertEquals(2, player.amount(1205))
        assertEquals(780, player.skills.experience(Skill.SMITHING))
        assertEquals(2, player.randomEventChecks)
        assertEquals(null, player.activeActionName())
        assertEquals(null, SmithingAnvilService.activeSelection(player))
    }
}
