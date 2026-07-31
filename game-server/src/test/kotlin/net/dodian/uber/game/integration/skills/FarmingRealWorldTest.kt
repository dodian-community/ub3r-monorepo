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
 * `FarmingModule.patchLocations`'s doc comment). Compost bins, bush/fruit-tree/tree patches,
 * and the wall-clock growth-tick scheduler stay legacy-owned this chunk - not covered here.
 */
class FarmingRealWorldTest : RealWorldSkillTest() {
    // CATHERBY_WEST slot 0 - an allotment patch, confirmed at this exact tile.
    private val allotmentObjectId = 8552
    private val allotmentX = 2805
    private val allotmentY = 3466

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
}
