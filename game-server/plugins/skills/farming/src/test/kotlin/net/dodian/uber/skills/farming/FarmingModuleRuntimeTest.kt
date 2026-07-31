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
    fun `compost bin object bindings route option 1 and option 5 to interactBin`() {
        val openBinding = FarmingModule.definition.objectBindings.single { it.option == 1 && 7837 in it.objectIds }
        val dumpBinding = FarmingModule.definition.objectBindings.single { it.option == 5 && 7837 in it.objectIds }
        val player = FakeSkillPlayer().apply { seedCompostBin("CATHERBY", compost = "COMPOST", state = "DONE") }

        openBinding.handler(net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction(player, 1, objRef(7837)))
        assertEquals("OPEN", player.compostBin("CATHERBY")?.state)

        dumpBinding.handler(net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction(player, 5, objRef(7837)))
        assertEquals(listOf("Yes", "No"), player.pendingDialogueOptionTexts())
    }

    @Test
    fun `item-on-object binding routes a compost item to useItemOnBin, not the patch logic`() {
        val binding = FarmingModule.definition.itemOnObjectBindings.single { 7837 in it.objectIds && 6055 in it.itemIds }
        val player = FakeSkillPlayer(mapOf(6055 to 1))

        val handled = binding.handler(net.dodian.uber.game.api.plugin.skills.SkillItemOnObjectInteraction(player, objRef(7837), 6055, 0, -1))

        assertTrue(handled)
        assertEquals("FILLED", player.compostBin("CATHERBY")?.state)
        assertEquals(0, player.amount(6055))
    }

    // --- Compost bins: fill cycle -----------------------------------------------------------

    @Test
    fun `filling an empty bin with a regular compost item sets FILLED and the regular tier`() {
        val player = FakeSkillPlayer(mapOf(6055 to 1))

        val handled = FarmingModule.useItemOnBin(player, 7837, 6055)

        assertTrue(handled)
        val bin = player.compostBin("CATHERBY")!!
        assertEquals("FILLED", bin.state)
        assertEquals("COMPOST", bin.compost)
        assertEquals(1, bin.amount)
        assertEquals(0, player.amount(6055))
    }

    @Test
    fun `filling an empty bin with a super compost item sets the super tier`() {
        val player = FakeSkillPlayer(mapOf(2114 to 1))

        FarmingModule.useItemOnBin(player, 7837, 2114)

        assertEquals("SUPERCOMPOST", player.compostBin("CATHERBY")?.compost)
    }

    @Test
    fun `filling a partially-filled bin with the same tier increments amount`() {
        val player = FakeSkillPlayer(mapOf(6055 to 1)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "FILLED", amount = 4)
        }

        FarmingModule.useItemOnBin(player, 7837, 6055)

        assertEquals(5, player.compostBin("CATHERBY")?.amount)
    }

    @Test
    fun `inserting a super compost item into a regular-compost bin asks to convert to regular`() {
        val player = FakeSkillPlayer(mapOf(2114 to 1)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "FILLED", amount = 3)
        }

        val handled = FarmingModule.useItemOnBin(player, 7837, 2114)

        assertTrue(handled)
        assertEquals(listOf("Yes", "No"), player.pendingDialogueOptionTexts())
        assertEquals(3, player.compostBin("CATHERBY")?.amount, "nothing consumed until an option is picked")
    }

    @Test
    fun `accepting the cross-tier super-into-regular conversion fills as plain COMPOST`() {
        val player = FakeSkillPlayer(mapOf(2114 to 1)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "FILLED", amount = 3)
        }

        FarmingModule.useItemOnBin(player, 7837, 2114)
        assertTrue(player.selectDialogueOption("Yes"))

        val bin = player.compostBin("CATHERBY")!!
        assertEquals("COMPOST", bin.compost, "legacy hardcodes both cross-tier confirmations to plain COMPOST")
        assertEquals(4, bin.amount)
        assertEquals(0, player.amount(2114))
    }

    @Test
    fun `declining the cross-tier conversion leaves the bin and inventory untouched`() {
        val player = FakeSkillPlayer(mapOf(2114 to 1)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "FILLED", amount = 3)
        }

        FarmingModule.useItemOnBin(player, 7837, 2114)
        assertTrue(player.selectDialogueOption("No"))

        val bin = player.compostBin("CATHERBY")!!
        assertEquals("COMPOST", bin.compost)
        assertEquals(3, bin.amount)
        assertEquals(1, player.amount(2114))
    }

    @Test
    fun `inserting a regular compost item into a supercompost bin asks to convert to regular`() {
        val player = FakeSkillPlayer(mapOf(6055 to 1)).apply {
            seedCompostBin("CATHERBY", compost = "SUPERCOMPOST", state = "FILLED", amount = 2)
        }

        FarmingModule.useItemOnBin(player, 7837, 6055)
        assertTrue(player.selectDialogueOption("Yes"))

        val bin = player.compostBin("CATHERBY")!!
        assertEquals("COMPOST", bin.compost)
        assertEquals(3, bin.amount)
    }

    @Test
    fun `a closed bin refuses new items with the rotting message`() {
        val player = FakeSkillPlayer(mapOf(6055 to 1)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "CLOSED", amount = 15)
        }

        val handled = FarmingModule.useItemOnBin(player, 7837, 6055)

        assertFalse(handled)
        assertEquals("The bin is currently in the process of rotting the containment.", player.messages.single())
    }

    @Test
    fun `a full bin refuses new items with the currently-full message`() {
        val player = FakeSkillPlayer(mapOf(6055 to 1)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "FILLED", amount = 15)
        }

        FarmingModule.useItemOnBin(player, 7837, 6055)

        assertEquals("The bin is currently full!", player.messages.single())
    }

    @Test
    fun `an open bin with leftover compost refuses new items until emptied`() {
        val player = FakeSkillPlayer(mapOf(6055 to 1)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "OPEN", amount = 3)
        }

        FarmingModule.useItemOnBin(player, 7837, 6055)

        assertEquals("Empty the bin before you try and fill it!", player.messages.single())
    }

    @Test
    fun `a done bin refuses new items and suggests opening it`() {
        val player = FakeSkillPlayer(mapOf(6055 to 1)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "DONE", amount = 15)
        }

        FarmingModule.useItemOnBin(player, 7837, 6055)

        assertEquals("The bin is done rotting the containment; Perhaps you should open it?", player.messages.single())
    }

    @Test
    fun `an item that is not compost has no use on the bin`() {
        val player = FakeSkillPlayer(mapOf(952 to 1)) // spade

        val handled = FarmingModule.useItemOnBin(player, 7837, 952)

        assertFalse(handled)
        assertEquals("This item has no use to be put into the bin.", player.messages.single())
    }

    // --- Compost bins: option-1 open/extract cycle -------------------------------------------

    @Test
    fun `option 1 on a full filled bin starts it rotting`() {
        val player = FakeSkillPlayer().apply { seedCompostBin("CATHERBY", compost = "COMPOST", state = "FILLED", amount = 15) }

        FarmingModule.interactBin(player, 7837, 1)

        assertEquals("CLOSED", player.compostBin("CATHERBY")?.state)
        assertTrue(player.farmingStateDirty)
        assertEquals(1, player.farmingRefreshVisualsCalls)
    }

    @Test
    fun `option 1 on a done bin opens it for extraction`() {
        val player = FakeSkillPlayer().apply { seedCompostBin("CATHERBY", compost = "COMPOST", state = "DONE", amount = 15) }

        FarmingModule.interactBin(player, 7837, 1)

        assertEquals("OPEN", player.compostBin("CATHERBY")?.state)
    }

    @Test
    fun `option 1 on an open bin with a bucket extracts one unit of compost`() {
        val player = FakeSkillPlayer(mapOf(1925 to 1)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "OPEN", amount = 5)
        }

        FarmingModule.interactBin(player, 7837, 1)

        assertEquals(4, player.compostBin("CATHERBY")?.amount)
        assertEquals("OPEN", player.compostBin("CATHERBY")?.state)
        assertEquals(0, player.amount(1925))
        assertEquals(1, player.amount(6032)) // COMPOST_ITEM_ID granted
    }

    @Test
    fun `extracting the last unit of compost resets the bin to empty`() {
        val player = FakeSkillPlayer(mapOf(1925 to 1)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "OPEN", amount = 1)
        }

        FarmingModule.interactBin(player, 7837, 1)

        val bin = player.compostBin("CATHERBY")!!
        assertEquals("EMPTY", bin.state)
        assertEquals("NONE", bin.compost)
        assertEquals(0, bin.amount)
    }

    @Test
    fun `extracting without a bucket shows a message`() {
        val player = FakeSkillPlayer().apply { seedCompostBin("CATHERBY", compost = "COMPOST", state = "OPEN", amount = 5) }

        FarmingModule.interactBin(player, 7837, 1)

        assertEquals("You are missing a bucket to be filled with compost.", player.messages.single())
        assertEquals(5, player.compostBin("CATHERBY")?.amount)
    }

    @Test
    fun `using a bucket directly on an open bin extracts, same as option 1`() {
        val player = FakeSkillPlayer(mapOf(1925 to 1)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "OPEN", amount = 5)
        }

        val handled = FarmingModule.useItemOnBin(player, 7837, 1925)

        assertTrue(handled)
        assertEquals(4, player.compostBin("CATHERBY")?.amount)
        assertEquals(1, player.amount(6032))
    }

    // --- Compost bins: dump dialogue (option 5) -----------------------------------------------

    @Test
    fun `dumping a bin with Yes clears it but leaves amount stale, matching the legacy quirk`() {
        val player = FakeSkillPlayer().apply { seedCompostBin("CATHERBY", compost = "COMPOST", state = "FILLED", amount = 10) }

        FarmingModule.interactBin(player, 7837, 5)
        assertTrue(player.selectDialogueOption("Yes"))

        val bin = player.compostBin("CATHERBY")!!
        assertEquals("NONE", bin.compost)
        assertEquals("EMPTY", bin.state)
        assertEquals(10, bin.amount, "legacy never explicitly clears amount on dump - preserved verbatim")
        assertEquals("You dump all the content inside the bin!", player.messages.single())
    }

    @Test
    fun `declining the dump dialogue leaves the bin untouched`() {
        val player = FakeSkillPlayer().apply { seedCompostBin("CATHERBY", compost = "COMPOST", state = "FILLED", amount = 10) }

        FarmingModule.interactBin(player, 7837, 5)
        assertTrue(player.selectDialogueOption("No"))

        val bin = player.compostBin("CATHERBY")!!
        assertEquals("COMPOST", bin.compost)
        assertEquals("FILLED", bin.state)
        assertEquals(10, bin.amount)
        assertTrue(player.messages.isEmpty())
    }

    // --- Compost bins: volcanic ash -> ultra compost -------------------------------------------

    @Test
    fun `volcanic ash converts a full open bin to ultra compost`() {
        val player = FakeSkillPlayer(mapOf(21622 to 25)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "OPEN", amount = 15)
        }

        val handled = FarmingModule.useItemOnBin(player, 7837, 21622)

        assertTrue(handled)
        assertEquals("ULTRACOMPOST", player.compostBin("CATHERBY")?.compost)
        assertEquals(0, player.amount(21622))
    }

    @Test
    fun `volcanic ash without 25 in inventory shows a message and does not convert`() {
        val player = FakeSkillPlayer(mapOf(21622 to 10)).apply {
            seedCompostBin("CATHERBY", compost = "COMPOST", state = "OPEN", amount = 15)
        }

        FarmingModule.useItemOnBin(player, 7837, 21622)

        assertEquals("You need 25 item 21622 in order to convert into ultra compost.", player.messages.single())
        assertEquals("COMPOST", player.compostBin("CATHERBY")?.compost)
        assertEquals(10, player.amount(21622))
    }

    // --- Compost bins: examine (op10, wired from PlayerCore.examineObject) -------------------

    @Test
    fun `examining a fresh (unseeded) bin reports it as empty`() {
        val player = FakeSkillPlayer()

        FarmingModule.examineBin(player, 7837)

        assertEquals("The bin is currently empty.", player.messages.single())
    }

    @Test
    fun `examining a closed bin reports it as rotting`() {
        val player = FakeSkillPlayer().apply { seedCompostBin("CATHERBY", compost = "COMPOST", state = "CLOSED", amount = 15) }

        FarmingModule.examineBin(player, 7837)

        assertEquals("The bin is currently in the process of rotting the containment.", player.messages.single())
    }

    @Test
    fun `examining a done bin reports the compost type is ready`() {
        val player = FakeSkillPlayer().apply { seedCompostBin("CATHERBY", compost = "COMPOST", state = "DONE", amount = 15) }

        FarmingModule.examineBin(player, 7837)

        assertEquals("The compost is ready.", player.messages.single())
    }

    @Test
    fun `examining an open bin reports the remaining amount`() {
        val player = FakeSkillPlayer().apply { seedCompostBin("CATHERBY", compost = "SUPERCOMPOST", state = "OPEN", amount = 5) }

        FarmingModule.examineBin(player, 7837)

        assertEquals("There is currently 5/15 supercompost remaining.", player.messages.single())
    }

    @Test
    fun `examining a filled bin reports the filled amount`() {
        val player = FakeSkillPlayer().apply { seedCompostBin("CATHERBY", compost = "COMPOST", state = "FILLED", amount = 3) }

        FarmingModule.examineBin(player, 7837)

        assertEquals("There is currently 3/15 compost filled.", player.messages.single())
    }

    // --- objectId -> patch type table ------------------------------------------------------
    // Confirmed by cross-referencing this server's own cache-decoded object positions against
    // the Tarnish reference server's exact zone rectangles (see FarmingModule.patchLocations'
    // doc comment and the migration plan/memory for the full derivation).

    @Test
    fun `live patch object ids cover all 6 categories including the corrected bush-fruit-tree-tree locations`() {
        // 12 allotment/flower/herb (the 3 previously-standalone-herb ids are now correctly
        // classified as fruit-tree/bush, not herb) + 13 bush/fruit-tree/tree = 25.
        assertEquals(25, FarmingModule.livePatchObjectIds.size)
        // 8388 (Taverley) and 19147 (Gnome Stronghold South) were previously excluded as
        // "unclassifiable bush-sized" - confirmed tree patches via the 2009scape object-id
        // range cross-reference, now live.
        assertTrue(8388 in FarmingModule.livePatchObjectIds)
        assertTrue(19147 in FarmingModule.livePatchObjectIds)
    }

    @Test
    fun `the 3 previously-misclassified standalone locations now resolve to their real crop type`() {
        // Confirmed via the 2009scape object-id range cross-reference (Farming part 3) - none
        // of these 3 were ever in the herb id range (8150-8156) at all. Behavioral check: a
        // fruit-tree sapling plants successfully at 7965, a herb seed does not.
        val herbSeedPlayer = FakeSkillPlayer(mapOf(5291 to 1, 5343 to 1)).apply { // guam herb seed
            setLevel(Skill.FARMING, 99)
            seedPatchSlot("CATHERBY_EAST", 0, state = "WEED", stageOrLife = 3)
        }
        FarmingModule.useItem(herbSeedPlayer, 7965, 5291)
        assertTrue(herbSeedPlayer.messages.single().endsWith("can only be planted at a herb patch."))

        val appleSaplingPlayer = FakeSkillPlayer(mapOf(5496 to 1, 952 to 1)).apply { // apple sapling + spade
            setLevel(Skill.FARMING, 99)
            seedPatchSlot("CATHERBY_EAST", 0, state = "WEED", stageOrLife = 3)
        }
        FarmingModule.useItem(appleSaplingPlayer, 7965, 5496)
        assertEquals("GROWING", appleSaplingPlayer.patchSlot("CATHERBY_EAST", 0)?.state)
        assertEquals(0, appleSaplingPlayer.amount(5496))
        assertEquals(1, appleSaplingPlayer.amount(5350)) // empty plant pot returned
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

    // --- Growth pulse simulation ---------------------------------------------------------------
    // FakeSkillPlayer.random.chance(numerator, denominator) is deterministic: numerator > 0
    // always succeeds. Real crop diseaseChance values (180-340 base, scaled by level/compost)
    // never round to exactly 0, so every non-final-stage growth transition below deterministically
    // rolls disease in these tests - that's exercised deliberately, not worked around.

    @Test
    fun `weed regrowth only decrements stageOrLife on the third pulse`() {
        val player = FakeSkillPlayer().apply { seedPatchSlot("CATHERBY_WEST", 0, state = "WEED", stageOrLife = 2, progress = 0) }

        FarmingModule.applyGrowthPulse(player)
        assertEquals(2, player.patchSlot("CATHERBY_WEST", 0)?.stageOrLife)
        assertEquals(1, player.patchSlot("CATHERBY_WEST", 0)?.progress)
        assertEquals(0, player.farmingNotifyInteractionCalls)

        FarmingModule.applyGrowthPulse(player)
        assertEquals(2, player.patchSlot("CATHERBY_WEST", 0)?.stageOrLife)
        assertEquals(2, player.patchSlot("CATHERBY_WEST", 0)?.progress)
        assertEquals(0, player.farmingNotifyInteractionCalls)

        FarmingModule.applyGrowthPulse(player)
        val slot = player.patchSlot("CATHERBY_WEST", 0)!!
        assertEquals(1, slot.stageOrLife)
        assertEquals(0, slot.progress)
        assertEquals(1, player.farmingNotifyInteractionCalls)
        assertEquals(1, player.farmingRefreshVisualsCalls)
    }

    @Test
    fun `a growing patch below its final stage rolls disease once growTick is reached`() {
        // potato: growTick=8, stages=5
        val player = FakeSkillPlayer().apply {
            seedPatchSlot("CATHERBY_WEST", 0, itemId = 5318, state = "GROWING", stageOrLife = 1, progress = 7)
        }

        FarmingModule.applyGrowthPulse(player)

        val slot = player.patchSlot("CATHERBY_WEST", 0)!!
        assertEquals("DISEASE", slot.state)
        assertEquals(2, slot.stageOrLife)
        assertEquals(0, slot.progress)
        assertEquals(1, player.farmingNotifyInteractionCalls)
    }

    @Test
    fun `a watered patch resolves through growing en route to the disease roll`() {
        val player = FakeSkillPlayer().apply {
            seedPatchSlot("CATHERBY_WEST", 0, itemId = 5318, state = "WATER", stageOrLife = 1, progress = 7)
        }

        FarmingModule.applyGrowthPulse(player)

        val slot = player.patchSlot("CATHERBY_WEST", 0)!!
        assertEquals("DISEASE", slot.state)
        assertEquals(2, slot.stageOrLife)
    }

    @Test
    fun `reaching the final stage bypasses the disease roll and harvests an allotment crop with life 3 plus compost tier`() {
        val player = FakeSkillPlayer().apply {
            seedPatchSlot("CATHERBY_WEST", 0, itemId = 5318, state = "GROWING", stageOrLife = 4, progress = 7, compost = "SUPERCOMPOST")
        }

        FarmingModule.applyGrowthPulse(player)

        val slot = player.patchSlot("CATHERBY_WEST", 0)!!
        assertEquals("HARVEST", slot.state)
        assertEquals(5, slot.stageOrLife) // 3 + compostTierOrdinal(SUPERCOMPOST)=2
    }

    @Test
    fun `reaching the final stage harvests a flower crop with life 1 regardless of compost`() {
        val player = FakeSkillPlayer().apply {
            seedPatchSlot("CATHERBY_WEST", 2, itemId = 5096, state = "GROWING", stageOrLife = 4, progress = 3, compost = "ULTRACOMPOST")
        }

        FarmingModule.applyGrowthPulse(player)

        val slot = player.patchSlot("CATHERBY_WEST", 2)!!
        assertEquals("HARVEST", slot.state)
        assertEquals(1, slot.stageOrLife)
    }

    @Test
    fun `a diseased patch dies after growTick times 2 pulses`() {
        val player = FakeSkillPlayer().apply {
            seedPatchSlot("CATHERBY_WEST", 0, itemId = 5318, state = "DISEASE", stageOrLife = 2, progress = 0)
        }

        repeat(15) { FarmingModule.applyGrowthPulse(player) }
        assertEquals("DISEASE", player.patchSlot("CATHERBY_WEST", 0)?.state)
        assertEquals(15, player.patchSlot("CATHERBY_WEST", 0)?.progress)

        FarmingModule.applyGrowthPulse(player) // 16th pulse = growTick(8) * 2

        val slot = player.patchSlot("CATHERBY_WEST", 0)!!
        assertEquals("DEAD", slot.state)
        assertEquals(0, slot.progress)
    }

    @Test
    fun `a closed 1-tick compost bin becomes done after a single pulse`() {
        val player = FakeSkillPlayer().apply { seedCompostBin("CATHERBY", compost = "COMPOST", state = "CLOSED", amount = 15, progress = 0) }

        FarmingModule.applyGrowthPulse(player)

        val bin = player.compostBin("CATHERBY")!!
        assertEquals("DONE", bin.state)
        assertEquals(0, bin.progress)
        assertEquals(1, player.farmingNotifyInteractionCalls)
    }

    @Test
    fun `falador's 60-tick compost bin only becomes done on the 60th pulse`() {
        val player = FakeSkillPlayer().apply { seedCompostBin("FALADOR", compost = "COMPOST", state = "CLOSED", amount = 15, progress = 0) }

        repeat(59) { FarmingModule.applyGrowthPulse(player) }
        assertEquals("CLOSED", player.compostBin("FALADOR")?.state)
        assertEquals(59, player.compostBin("FALADOR")?.progress)

        FarmingModule.applyGrowthPulse(player)

        val bin = player.compostBin("FALADOR")!!
        assertEquals("DONE", bin.state)
        assertEquals(0, bin.progress)
    }

    // --- Bush/fruit-tree/tree patches (Farming part 3) -----------------------------------------
    // redberry bush: seed 5101, growTick=16, checkHealthXp=256, stages=6, harvestItem=1951 @ ARDOUGNE_SOUTH/7580
    // apple tree: sapling 5496, growTick=120, checkHealthXp=5090, stages=7, harvestItem=1955 @ CATHERBY_EAST/7965
    // oak tree: sapling 5370, growTick=30, checkHealthXp=1860, harvestItem=1521 (0 harvestXp) @ TAVERLY_SOUTH/8388

    @Test
    fun `planting a bush seed consumes the seed and returns no plant pot`() {
        val player = FakeSkillPlayer(mapOf(5101 to 1, 5343 to 1)).apply { // redberry seed + dibber
            setLevel(Skill.FARMING, 99)
            seedPatchSlot("ARDOUGNE_SOUTH", 0, state = "WEED", stageOrLife = 3)
        }

        val handled = FarmingModule.useItem(player, 7580, 5101)

        assertTrue(handled)
        assertEquals("GROWING", player.patchSlot("ARDOUGNE_SOUTH", 0)?.state)
        assertEquals(0, player.amount(5101))
        assertEquals(0, player.amount(5350)) // no empty plant pot for bush
    }

    @Test
    fun `reaching harvest on a checkHealthXp crop checks health instead of collecting, entering PRODUCTION`() {
        val player = FakeSkillPlayer().apply {
            setLevel(Skill.FARMING, 50)
            seedPatchSlot("ARDOUGNE_SOUTH", 0, itemId = 5101, state = "HARVEST", stageOrLife = 1)
        }

        val handled = FarmingModule.clickPatch(player, 7580)

        assertTrue(handled)
        val slot = player.patchSlot("ARDOUGNE_SOUTH", 0)!!
        assertEquals("PRODUCTION", slot.state)
        assertEquals(4, slot.stageOrLife) // bush life
        assertEquals(256, player.skills.experience(Skill.FARMING))
        assertTrue(player.messages.single().startsWith("You check the health of the redberry"))
    }

    @Test
    fun `PRODUCTION harvest decrements life and grants produce for a bush`() {
        val player = FakeSkillPlayer().apply { seedPatchSlot("ARDOUGNE_SOUTH", 0, itemId = 5101, state = "PRODUCTION", stageOrLife = 4) }

        FarmingModule.clickPatch(player, 7580)

        val slot = player.patchSlot("ARDOUGNE_SOUTH", 0)!!
        assertEquals("PRODUCTION", slot.state)
        assertEquals(3, slot.stageOrLife)
        assertEquals(1, player.amount(1951))
        assertEquals(20, player.skills.experience(Skill.FARMING))
    }

    @Test
    fun `clicking a tree in PRODUCTION always stumps it regardless of remaining life`() {
        val player = FakeSkillPlayer().apply { seedPatchSlot("TAVERLY_SOUTH", 0, itemId = 5370, state = "PRODUCTION", stageOrLife = 1) }

        FarmingModule.clickPatch(player, 8388)

        assertEquals("STUMP", player.patchSlot("TAVERLY_SOUTH", 0)?.state)
    }

    @Test
    fun `an exhausted bush in PRODUCTION resets fully to a fresh weedy patch, not a stump`() {
        val player = FakeSkillPlayer().apply { seedPatchSlot("ARDOUGNE_SOUTH", 0, itemId = 5101, state = "PRODUCTION", stageOrLife = 0) }

        FarmingModule.clickPatch(player, 7580)

        val slot = player.patchSlot("ARDOUGNE_SOUTH", 0)!!
        assertEquals(-1, slot.itemId)
        assertEquals("WEED", slot.state)
        assertEquals(3, slot.stageOrLife)
    }

    @Test
    fun `an exhausted fruit tree in PRODUCTION stumps`() {
        val player = FakeSkillPlayer().apply { seedPatchSlot("CATHERBY_EAST", 0, itemId = 5496, state = "PRODUCTION", stageOrLife = 0) }

        FarmingModule.clickPatch(player, 7965)

        assertEquals("STUMP", player.patchSlot("CATHERBY_EAST", 0)?.state)
    }

    @Test
    fun `secateurs cure a diseased checkHealthXp crop and grant item 6020 regardless of outcome`() {
        // FakeSkillPlayer.random.chance(numerator, denominator) is numerator > 0, deterministically
        // true here - only the success path is directly observable via the fake, matching the
        // same convention noted for the disease-roll tests above.
        val player = FakeSkillPlayer(mapOf(5329 to 1)).apply { // secateurs
            seedPatchSlot("ARDOUGNE_SOUTH", 0, itemId = 5101, state = "DISEASE", stageOrLife = 2, progress = 5)
        }

        FarmingModule.clickPatch(player, 7580)

        val slot = player.patchSlot("ARDOUGNE_SOUTH", 0)!!
        assertEquals("GROWING", slot.state)
        assertEquals(0, slot.progress)
        assertEquals(1, player.amount(6020))
    }

    @Test
    fun `secateurs cure without the tool shows a message`() {
        val player = FakeSkillPlayer().apply { seedPatchSlot("ARDOUGNE_SOUTH", 0, itemId = 5101, state = "DISEASE", stageOrLife = 2) }

        FarmingModule.clickPatch(player, 7580)

        assertEquals("You need to use a pair of secateurs to prune the tree.", player.messages.single())
    }

    @Test
    fun `a stump regrows into PRODUCTION after growTick over 3 pulses`() {
        val player = FakeSkillPlayer().apply { seedPatchSlot("TAVERLY_SOUTH", 0, itemId = 5370, state = "STUMP", stageOrLife = 0, progress = 0) }

        repeat(9) { FarmingModule.applyGrowthPulse(player) } // oak growTick=30, 30/3=10
        assertEquals("STUMP", player.patchSlot("TAVERLY_SOUTH", 0)?.state)

        FarmingModule.applyGrowthPulse(player)

        val slot = player.patchSlot("TAVERLY_SOUTH", 0)!!
        assertEquals("PRODUCTION", slot.state)
        assertEquals(0, slot.progress)
    }

    @Test
    fun `PRODUCTION regrows stageOrLife back up to the fresh per-type life cap`() {
        val player = FakeSkillPlayer().apply { seedPatchSlot("ARDOUGNE_SOUTH", 0, itemId = 5101, state = "PRODUCTION", stageOrLife = 3, progress = 0) }

        repeat(4) { FarmingModule.applyGrowthPulse(player) } // redberry growTick=16, integer 16/3=5 -> the 5th pulse triggers
        assertEquals(3, player.patchSlot("ARDOUGNE_SOUTH", 0)?.stageOrLife)

        FarmingModule.applyGrowthPulse(player)

        assertEquals(4, player.patchSlot("ARDOUGNE_SOUTH", 0)?.stageOrLife) // bush life cap
    }

    @Test
    fun `PRODUCTION stops regrowing once it reaches the life cap`() {
        val player = FakeSkillPlayer().apply { seedPatchSlot("ARDOUGNE_SOUTH", 0, itemId = 5101, state = "PRODUCTION", stageOrLife = 4, progress = 0) }

        repeat(20) { FarmingModule.applyGrowthPulse(player) }

        assertEquals(4, player.patchSlot("ARDOUGNE_SOUTH", 0)?.stageOrLife)
        assertEquals(0, player.patchSlot("ARDOUGNE_SOUTH", 0)?.progress, "should never accumulate progress once at the cap")
    }
}

private fun objRef(id: Int) = net.dodian.uber.game.api.plugin.skills.SkillObjectRef(id, net.dodian.uber.game.api.plugin.skills.SkillPosition(0, 0, 0))
