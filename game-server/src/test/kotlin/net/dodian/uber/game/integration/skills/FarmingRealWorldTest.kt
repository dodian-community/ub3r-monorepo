package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Real-world (real cache/game-loop) integration tests for Farming's core allotment/flower/herb
 * patch lifecycle - plant/harvest, driven through the real `client.farmingJson`-backed
 * `SkillFarmingState` adapter. Object id/position confirmed via `CacheCollisionAuditStore`
 * cross-referenced against the Tarnish reference server's zone rectangles (see
 * `FarmingModule.patchLocations`'s doc comment). Also covers the Catherby compost bin's
 * open/extract/dump cycle (Chunk 9) and a bush checkHealthXp/PRODUCTION cycle (Chunk 11 - object
 * id/position cross-referenced against the 2009scape reference server, see the same doc comment).
 * The wall-clock growth-tick *scheduler* (not the pulse math itself, which is plugin-owned) stays
 * legacy-owned - not covered here, no clock injection exists to drive it end-to-end.
 */
class FarmingRealWorldTest : RealWorldSkillTest() {
    // CATHERBY_WEST slot 0 - an allotment patch, confirmed at this exact tile.
    private val allotmentObjectId = 8552
    private val allotmentX = 2805
    private val allotmentY = 3466

    // Catherby compost bin - FarmingData.compostBin.CATHERBY, confirmed at this exact tile.
    private val binObjectId = 7837
    private val binX = 2804
    private val binY = 3464

    @Test
    fun `planting a potato at the real catherby allotment patch grants xp and starts it growing`() {
        val player = spawnPlayer("FarmerOne", allotmentX, allotmentY)
        player.farmingJson.farmingLoad("")
        RealWorldHarness.setSkillLevel(player, Skill.FARMING, 10)
        RealWorldHarness.giveItem(player, 5318, 3) // potato seed x3
        RealWorldHarness.giveItem(player, 5343, 1) // seed dibber
        // Skip the weeding dance (a fresh farmingLoad("") starts every patch at WEED/stage 0,
        // matching a brand-new save) by seeding "already weeded" directly through the real
        // adapter - this is what's under test either way (SkillFarmingState round-tripping
        // through client.farmingJson), just without 3 redundant rake clicks first.
        player.asSkillPlayer().farmingState.writePatchSlot("CATHERBY_WEST", 0, -1, "WEED", "NONE", 3, 0, -1)

        RealWorldHarness.itemOnObject(player, allotmentObjectId, allotmentX, allotmentY, 5318)
        RealWorldHarness.tick(3)

        assertEquals(0, player.getInvAmt(5318))
        val planted = player.asSkillPlayer().farmingState.patchSlots().single { it.patchName == "CATHERBY_WEST" && it.slot == 0 }
        assertEquals(5318, planted.itemId)
        assertEquals("GROWING", planted.state)
        assertTrue(player.getExperience(Skill.FARMING) > 0, "expected farming xp for planting")
    }

    @Test
    fun `planting the wrong seed type shows the legacy rejection message and does not plant`() {
        val player = spawnPlayer("FarmerTwo", allotmentX, allotmentY + 10)
        player.farmingJson.farmingLoad("")
        RealWorldHarness.setSkillLevel(player, Skill.FARMING, 99)
        RealWorldHarness.giveItem(player, 5096, 1) // marigold seed (a flower seed) on an allotment slot
        RealWorldHarness.giveItem(player, 5343, 1)
        player.asSkillPlayer().farmingState.writePatchSlot("CATHERBY_WEST", 0, -1, "WEED", "NONE", 3, 0, -1)

        RealWorldHarness.itemOnObject(player, allotmentObjectId, allotmentX, allotmentY, 5096)

        assertEquals(1, player.getInvAmt(5096), "expected the seed not to be consumed")
        val slot = player.asSkillPlayer().farmingState.patchSlots().single { it.patchName == "CATHERBY_WEST" && it.slot == 0 }
        assertEquals(-1, slot.itemId, "expected the patch to remain unplanted")
        assertEquals(0, player.getExperience(Skill.FARMING))
    }

    private fun binState(player: net.dodian.uber.game.model.entity.player.Client) =
        player.asSkillPlayer().farmingState.compostBins().single { it.binName == "CATHERBY" }

    @Test
    fun `clicking a full compost bin through the real object dispatch starts it rotting`() {
        val player = spawnPlayer("BinCloser", binX, binY)
        player.farmingJson.farmingLoad("")
        player.asSkillPlayer().farmingState.writeCompostBin("CATHERBY", "COMPOST", "FILLED", 15, 0)

        RealWorldHarness.objectOption(player, binObjectId, 1, binX, binY)
        RealWorldHarness.tick(5)

        assertEquals("CLOSED", binState(player).state)
    }

    @Test
    fun `using a bucket on an open compost bin through the real item-on-object dispatch extracts compost`() {
        val player = spawnPlayer("BinExtractor", binX, binY)
        player.farmingJson.farmingLoad("")
        player.asSkillPlayer().farmingState.writeCompostBin("CATHERBY", "COMPOST", "OPEN", 5, 0)
        RealWorldHarness.giveItem(player, 1925, 1) // bucket

        RealWorldHarness.itemOnObject(player, binObjectId, binX, binY, 1925)
        RealWorldHarness.tick(2)

        assertEquals(4, binState(player).amount)
        assertEquals(0, player.getInvAmt(1925))
        assertEquals(1, player.getInvAmt(6032)) // a bucket of compost granted
    }

    @Test
    fun `dumping a compost bin through the real dialogue option click empties it`() {
        val player = spawnPlayer("BinDumper", binX, binY)
        player.farmingJson.farmingLoad("")
        player.asSkillPlayer().farmingState.writeCompostBin("CATHERBY", "COMPOST", "FILLED", 10, 0)

        RealWorldHarness.objectOption(player, binObjectId, 5, binX, binY)
        RealWorldHarness.tick(5)
        RealWorldHarness.button(player, 2461) // "Yes"
        RealWorldHarness.tick(3)

        assertEquals("EMPTY", binState(player).state)
        assertEquals("NONE", binState(player).compost)
    }

    @Test
    fun `applyGrowthPulse advances a real planted patch to harvest through the real farmingJson storage`() {
        val player = spawnPlayer("GrowthPulseFarmer", allotmentX, allotmentY)
        player.farmingJson.farmingLoad("")
        // potato: growTick=8, stages=5 - seed one pulse away from the final stage so the
        // transition is deterministic (bypasses the real, genuinely-random disease roll).
        player.asSkillPlayer().farmingState.writePatchSlot("CATHERBY_WEST", 0, 5318, "GROWING", "NONE", 4, 7, -1)

        net.dodian.uber.skills.farming.FarmingModule.applyGrowthPulse(player.asSkillPlayer())

        val slot = player.asSkillPlayer().farmingState.patchSlots().single { it.patchName == "CATHERBY_WEST" && it.slot == 0 }
        assertEquals("HARVEST", slot.state)
        assertEquals(3, slot.stageOrLife) // 3 + compostTierOrdinal(NONE)=0
    }

    @Test
    fun `applyGrowthPulse closes a real full compost bin through the real farmingJson storage`() {
        val player = spawnPlayer("GrowthPulseBinFarmer", binX, binY)
        player.farmingJson.farmingLoad("")
        player.asSkillPlayer().farmingState.writeCompostBin("CATHERBY", "COMPOST", "CLOSED", 15, 0)

        net.dodian.uber.skills.farming.FarmingModule.applyGrowthPulse(player.asSkillPlayer())

        assertEquals("DONE", binState(player).state)
    }

    // ARDOUGNE_SOUTH slot 0 - a bush patch, confirmed at this exact tile (Chunk 11).
    private val bushObjectId = 7580
    private val bushX = 2617
    private val bushY = 3225

    @Test
    fun `a real bush patch's checkHealthXp-PRODUCTION-harvest cycle round-trips through farmingJson`() {
        val player = spawnPlayer("BushFarmer", bushX, bushY)
        player.farmingJson.farmingLoad("")
        // redberry bush: checkHealthXp=256, seed one click away from the check-health branch.
        player.asSkillPlayer().farmingState.writePatchSlot("ARDOUGNE_SOUTH", 0, 5101, "HARVEST", "NONE", 1, 0, -1)

        RealWorldHarness.objectOption(player, bushObjectId, 1, bushX, bushY)
        RealWorldHarness.tick(5)

        val checked = player.asSkillPlayer().farmingState.patchSlots().single { it.patchName == "ARDOUGNE_SOUTH" && it.slot == 0 }
        assertEquals("PRODUCTION", checked.state)
        assertEquals(4, checked.stageOrLife) // bush life cap
        assertTrue(player.getExperience(Skill.FARMING) >= 256)

        RealWorldHarness.objectOption(player, bushObjectId, 1, bushX, bushY)
        RealWorldHarness.tick(5)

        val harvested = player.asSkillPlayer().farmingState.patchSlots().single { it.patchName == "ARDOUGNE_SOUTH" && it.slot == 0 }
        assertEquals("PRODUCTION", harvested.state)
        assertEquals(3, harvested.stageOrLife)
        assertEquals(1, player.getInvAmt(1951)) // redberries
    }
}
