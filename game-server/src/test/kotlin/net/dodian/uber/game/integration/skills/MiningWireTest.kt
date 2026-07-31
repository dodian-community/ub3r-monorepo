package net.dodian.uber.game.integration.skills

import net.dodian.uber.game.integration.skills.support.RealWorldHarness
import net.dodian.uber.game.integration.skills.support.RealWorldSkillTest
import net.dodian.uber.game.integration.skills.support.WireDriver
import net.dodian.uber.game.model.player.skills.Skill
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Protocol-level integration test: drives a real client-shaped packet over the REAL wire
 * layer - encoded inbound frame -> GamePacketDecoder -> GamePacketHandler ->
 * InboundPacketMailbox -> EntityProcessor on real ticks - instead of calling packet
 * services directly like the RealWorldHarness tests do. This is the layer (codec /
 * opcode mapping / interface wiring) that neither the module-level FakeSkillPlayer tests
 * nor the direct-call harness tests ever exercise.
 *
 * Complements, does not replace, MiningRealWorldTest and the module tests. See
 * [WireDriver] for how the frames are encoded.
 */
class MiningWireTest : RealWorldSkillTest() {
    private val copperRockId = 10943
    private val copperOreId = 436
    private val bronzePickaxeId = 1265

    @Test
    fun `mining a rock over the real wire yields ore xp and a swing message`() {
        val player = spawnPlayer("WireMiner", 3230, 3218)
        WireDriver.wireClient(player)
        RealWorldHarness.setSkillLevel(player, Skill.MINING, 1)
        RealWorldHarness.giveItem(player, bronzePickaxeId, 1)
        spawnObject(copperRockId, 3230, 3219)

        WireDriver.objectOption(player, copperRockId, x = 3230, y = 3219, option = 1)
        val gotOre = RealWorldHarness.tickUntil(maxTicks = 50) { player.getInvAmt(copperOreId) > 0 }

        assertTrue(gotOre, "expected ore to be mined over the wire path within 50 ticks")
        assertTrue(player.getExperience(Skill.MINING) > 0, "expected mining xp to be gained")

        val messages = WireDriver.outboundMessages(player)
        assertTrue(
            messages.any { it.contains("You swing your pick") },
            "expected the swing message on the wire, got: $messages"
        )
    }
}
