package net.dodian.uber.skills.farming

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.testkit.FakeSkillPlayer
import net.dodian.uber.skills.testkit.LiveSkillModuleFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FarmingModuleRuntimeTest {
    @Test
    fun `descriptor and manifest are wired consistently`() {
        LiveSkillModuleFixture.requirePlugin(FarmingModule.descriptor.id, Skill.FARMING)
        assertEquals(FarmingModule.descriptor.id, FarmingModule.contentManifest.id)
    }

    @Test
    fun `toml crops are loaded`() {
        assertTrue(FarmingModule.crops.isNotEmpty())
        assertTrue(FarmingModule.crops.any { it.name == "potato" })
        assertEquals(44, FarmingModule.crops.size)
    }

    @Test
    fun `revision 218 empty farming patch roots remain explicitly owned`() {
        val expected = setOf(8573, 7840, 8132)
        assertEquals(expected, FarmingModule.patchObjectIds.toSet())
        expected.forEach { objectId ->
            assertTrue(FarmingModule.definition.objectBindings.any { it.option == 2 && objectId in it.objectIds })
            assertTrue(FarmingModule.definition.itemOnObjectBindings.any { objectId in it.objectIds })
        }
    }

    @Test
    fun `compost bin open and dump options route to SkillFarming interactBin`() {
        val openBinding = FarmingModule.definition.objectBindings.single { it.option == 1 && 7837 in it.objectIds }
        val dumpBinding = FarmingModule.definition.objectBindings.single { it.option == 5 && 7837 in it.objectIds }
        val player = FakeSkillPlayer()

        openBinding.handler(net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction(player, 1, objRef(7837)))
        dumpBinding.handler(net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction(player, 5, objRef(7837)))

        assertEquals(listOf(7837 to 1, 7837 to 5), player.farmingInteractBinCalls)
    }

    @Test
    fun `item used on a compost bin routes to SkillFarming useItemOnBin, not the patch logic`() {
        val binding = FarmingModule.definition.itemOnObjectBindings.single { 7837 in it.objectIds && 1925 in it.itemIds }
        val player = FakeSkillPlayer()

        val handled = binding.handler(net.dodian.uber.game.api.plugin.skills.SkillItemOnObjectInteraction(player, objRef(7837), 1925, 0, -1))

        assertTrue(handled)
        assertEquals(listOf(7837 to 1925), player.farmingUseItemOnBinCalls)
    }

    // --- objectId -> patch type table ------------------------------------------------------
    // Confirmed by cross-referencing this server's own cache-decoded object positions against
    // the Tarnish reference server's exact zone rectangles (see FarmingModule.patchLocations'
    // doc comment and the migration plan/memory for the full derivation).

    @Test
    fun `live patch object ids exclude the two bush-sized locations`() {
        // 8388 (Taverley) and 19147 (Gnome Stronghold South) are 3x3, bush-patch-sized -
        // confirmed misclassified in the old id list, excluded here as out of scope.
        assertFalse(8388 in FarmingModule.livePatchObjectIds)
        assertFalse(19147 in FarmingModule.livePatchObjectIds)
        assertEquals(15, FarmingModule.livePatchObjectIds.size)
    }

    @Test
    fun `catherby west's 4 patches resolve to the confirmed allotment-allotment-flower-herb order`() {
        val player = FakeSkillPlayer(mapOf(5318 to 3, 5343 to 1)).apply { setLevel(Skill.FARMING, 99) }
        // Plant a potato (allotment-only seed) on each of the 4 Catherby West ids; only the two
        // allotment ids (8552/8553) should accept it, the other two should reject it by type.
        // A fresh (never-seeded) fake patch starts at WEED/stage 0 (matching a brand-new,
        // never-touched save - see FarmingState.farmingLoad's "isNew" default), which needs
        // raking before it can be planted; explicitly seed stage 3 ("already weeded") to test
        // the planting branch specifically.
        listOf(0, 2, 3).forEach { player.seedPatchSlot("CATHERBY_WEST", it, state = "WEED", stageOrLife = 3) }

        FarmingModule.useItem(player, 8552, 5318)
        assertEquals("GROWING", player.patchSlot("CATHERBY_WEST", 0)?.state)

        FarmingModule.useItem(player, 7848, 5318)
        assertTrue(player.messages.last().endsWith("can only be planted at a allotment patch."))

        FarmingModule.useItem(player, 8151, 5318)
        assertTrue(player.messages.last().endsWith("can only be planted at a allotment patch."))
    }

    // --- Planting -----------------------------------------------------------------------------
    // FakeSkillPlayer.inventory.itemName(id) is a generic "item $id" placeholder (no real item
    // name table in the fake), so messages that embed an item's display name show that
    // placeholder rather than a real name - expected strings below match the fake's actual
    // output, not real OSRS item text.

    @Test
    fun `planting the right seed on the right empty patch succeeds`() {
        val player = FakeSkillPlayer(mapOf(5318 to 3, 5343 to 1)).apply {
            setLevel(Skill.FARMING, 10)
            seedPatchSlot("CATHERBY_WEST", 0, state = "WEED", stageOrLife = 3)
        }

        val handled = FarmingModule.useItem(player, 8552, 5318) // potato on an allotment slot

        assertTrue(handled)
        assertEquals(0, player.amount(5318))
        val slot = player.patchSlot("CATHERBY_WEST", 0)!!
        assertEquals(5318, slot.itemId)
        assertEquals("GROWING", slot.state)
        assertEquals(1, slot.stageOrLife)
        assertEquals(32, player.skills.experience(Skill.FARMING))
        assertEquals(1, player.farmingRefreshVisualsCalls)
        assertTrue(player.farmingStateDirty)
    }

    @Test
    fun `planting the wrong seed for a patch shows the legacy rejection message`() {
        val player = FakeSkillPlayer(mapOf(5096 to 1, 5343 to 1)).apply { // marigold (flower seed) on an allotment slot
            setLevel(Skill.FARMING, 99)
            seedPatchSlot("CATHERBY_WEST", 0, state = "WEED", stageOrLife = 3)
        }

        FarmingModule.useItem(player, 8552, 5096)

        assertEquals("item 5096 can only be planted at a flower patch.", player.messages.single())
        assertEquals(1, player.amount(5096))
    }

    @Test
    fun `planting without enough farming level shows a message and does not consume the seed`() {
        val player = FakeSkillPlayer(mapOf(5323 to 3, 5343 to 1)).apply { // strawberry needs level 31
            setLevel(Skill.FARMING, 1)
            seedPatchSlot("CATHERBY_WEST", 0, state = "WEED", stageOrLife = 3)
        }

        FarmingModule.useItem(player, 8552, 5323)

        assertEquals("You need level 31 farming to plant item 5323.", player.messages.single())
        assertEquals(3, player.amount(5323))
    }

    @Test
    fun `planting without a seed dibber shows a message`() {
        val player = FakeSkillPlayer(mapOf(5318 to 3)).apply {
            setLevel(Skill.FARMING, 10)
            seedPatchSlot("CATHERBY_WEST", 0, state = "WEED", stageOrLife = 3)
        }

        FarmingModule.useItem(player, 8552, 5318)

        assertEquals("You are missing the item 5343 tool.", player.messages.single())
    }

    @Test
    fun `planting an allotment seed without 3 in inventory shows a message`() {
        val player = FakeSkillPlayer(mapOf(5318 to 2, 5343 to 1)).apply {
            setLevel(Skill.FARMING, 10)
            seedPatchSlot("CATHERBY_WEST", 0, state = "WEED", stageOrLife = 3)
        }

        FarmingModule.useItem(player, 8552, 5318)

        assertEquals("You need 3 item 5318 to plant here.", player.messages.single())
    }

    // --- Weeding / dead --------------------------------------------------------------------

    @Test
    fun `raking a weedy patch with a rake advances the weed count`() {
        val player = FakeSkillPlayer(mapOf(5341 to 1)).apply { seedPatchSlot("CATHERBY_WEST", 0, state = "WEED", stageOrLife = 0) }

        val handled = FarmingModule.clickPatch(player, 8552)

        assertTrue(handled)
        assertEquals(1, player.patchSlot("CATHERBY_WEST", 0)?.stageOrLife)
        assertEquals(1, player.amount(6055)) // regularCompostItemIds[0], granted per legacy
    }

    @Test
    fun `raking without a rake shows a message`() {
        val player = FakeSkillPlayer().apply { seedPatchSlot("CATHERBY_WEST", 0, state = "WEED", stageOrLife = 0) }

        FarmingModule.clickPatch(player, 8552)

        assertEquals("You need a rake in order to clear the weed.", player.messages.single())
    }

    @Test
    fun `clearing a dead patch with a spade resets it to a fresh weedy patch`() {
        val player = FakeSkillPlayer(mapOf(952 to 1)).apply {
            seedPatchSlot("CATHERBY_WEST", 0, itemId = 5318, state = "DEAD", stageOrLife = 2)
        }

        FarmingModule.clickPatch(player, 8552)

        val slot = player.patchSlot("CATHERBY_WEST", 0)!!
        assertEquals(-1, slot.itemId)
        assertEquals("WEED", slot.state)
        assertEquals(3, slot.stageOrLife)
    }

    // --- Watering -----------------------------------------------------------------------------

    @Test
    fun `watering a growing allotment patch consumes a charge and sets the water state`() {
        val player = FakeSkillPlayer(mapOf(5333 to 1)).apply {
            seedPatchSlot("CATHERBY_WEST", 0, itemId = 5318, state = "GROWING", stageOrLife = 1)
        }

        FarmingModule.useItem(player, 8552, 5333) // "Watering can(2)"

        assertEquals(0, player.amount(5333))
        assertEquals(1, player.amount(5332)) // decremented charge (itemId - 1)
        assertEquals("WATER", player.patchSlot("CATHERBY_WEST", 0)?.state)
    }

    @Test
    fun `watering a herb patch has no effect - legacy only allows allotment and flower`() {
        val player = FakeSkillPlayer(mapOf(5333 to 1)).apply {
            seedPatchSlot("CATHERBY_WEST", 3, itemId = 5291, state = "GROWING", stageOrLife = 1) // guam on the herb slot
        }

        val handled = FarmingModule.useItem(player, 8151, 5333)

        assertTrue(handled)
        assertTrue(player.messages.isEmpty())
        assertEquals("GROWING", player.patchSlot("CATHERBY_WEST", 3)?.state)
        assertEquals(1, player.amount(5333), "expected the watering can to be untouched")
    }

    // --- Compost ------------------------------------------------------------------------------

    @Test
    fun `applying compost to an untreated patch upgrades it and grants a bucket`() {
        val player = FakeSkillPlayer(mapOf(6032 to 1)).apply { seedPatchSlot("CATHERBY_WEST", 0, state = "WEED", stageOrLife = 3) }

        FarmingModule.useItem(player, 8552, 6032)

        assertEquals(0, player.amount(6032))
        assertEquals(1, player.amount(1925))
        assertEquals("COMPOST", player.patchSlot("CATHERBY_WEST", 0)?.compost)
    }

    @Test
    fun `applying a lower compost tier than already present refuses with the legacy doubled-word message`() {
        val player = FakeSkillPlayer(mapOf(6032 to 1)).apply {
            seedPatchSlot("CATHERBY_WEST", 0, state = "WEED", stageOrLife = 3, compost = "SUPERCOMPOST")
        }

        FarmingModule.useItem(player, 8552, 6032)

        assertEquals("There is no point in using item 6032 when the patch already got supercompostcompost.", player.messages.single())
        assertEquals(1, player.amount(6032))
    }

    // --- Disease / harvest ----------------------------------------------------------------

    @Test
    fun `curing a diseased patch with plant cure returns it to growing`() {
        val player = FakeSkillPlayer(mapOf(6036 to 1)).apply {
            seedPatchSlot("CATHERBY_WEST", 0, itemId = 5318, state = "DISEASE", stageOrLife = 2, progress = 5)
        }

        FarmingModule.clickPatch(player, 8552)

        val slot = player.patchSlot("CATHERBY_WEST", 0)!!
        assertEquals("GROWING", slot.state)
        assertEquals(0, slot.progress)
        assertEquals(0, player.amount(6036))
    }

    @Test
    fun `harvesting with a spade and free inventory space grants produce and xp`() {
        val player = FakeSkillPlayer(mapOf(952 to 1)).apply {
            seedPatchSlot("CATHERBY_WEST", 0, itemId = 5318, state = "HARVEST", stageOrLife = 3) // potato, life=3
        }

        FarmingModule.clickPatch(player, 8552)

        assertEquals(1, player.amount(1942)) // potato item
        assertEquals(36, player.skills.experience(Skill.FARMING))
        assertEquals(2, player.patchSlot("CATHERBY_WEST", 0)?.stageOrLife)
    }

    @Test
    fun `harvesting the last life clears the patch back to fresh weeds`() {
        val player = FakeSkillPlayer(mapOf(952 to 1)).apply {
            seedPatchSlot("CATHERBY_WEST", 0, itemId = 5318, state = "HARVEST", stageOrLife = 1)
        }

        FarmingModule.clickPatch(player, 8552)

        val slot = player.patchSlot("CATHERBY_WEST", 0)!!
        assertEquals(-1, slot.itemId)
        assertEquals("WEED", slot.state)
    }

    @Test
    fun `harvesting without free inventory space shows a message`() {
        val fullInventory = (mapOf(952 to 1) + (2..28).associateWith { 1 }) // spade + 27 filler items = 28 slots
        val player = FakeSkillPlayer(fullInventory).apply {
            seedPatchSlot("CATHERBY_WEST", 0, itemId = 5318, state = "HARVEST", stageOrLife = 3)
        }

        FarmingModule.clickPatch(player, 8552)

        assertEquals("You do not have enough inventory space!", player.messages.single())
    }

    // --- Inspect ------------------------------------------------------------------------------

    @Test
    fun `inspecting an empty patch reports it as empty`() {
        val player = FakeSkillPlayer().apply { seedPatchSlot("CATHERBY_WEST", 0, state = "WEED", stageOrLife = 3) }

        FarmingModule.inspectPatch(player, 8552)

        assertEquals("This is a allotment patch. The soil has not been treated. The patch is empty.", player.messages.single())
    }

    @Test
    fun `inspecting a growing patch reports its stage`() {
        val player = FakeSkillPlayer().apply {
            seedPatchSlot("CATHERBY_WEST", 0, itemId = 5318, state = "GROWING", stageOrLife = 2, compost = "COMPOST")
        }

        FarmingModule.inspectPatch(player, 8552)

        assertEquals(
            "This is a allotment patch. The soil has been treated with compost. The patch has potato growing in it and is at state 2/5",
            player.messages.single(),
        )
    }
}

private fun objRef(id: Int) = net.dodian.uber.game.api.plugin.skills.SkillObjectRef(id, net.dodian.uber.game.api.plugin.skills.SkillPosition(0, 0, 0))
