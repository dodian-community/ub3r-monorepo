package net.dodian.uber.game.skills.mining

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MiningServiceBehaviorTest {
    @Test
    fun `mining delay formula remains stable for dragon boost and non boost`() {
        val client = Client(EmbeddedChannel(), 1)
        client.setLevel(61, net.dodian.uber.game.model.player.skills.Skill.MINING)
        val dragonPick = MiningData.pickaxeByItemId.getValue(11920)
        val runite = MiningData.rockByObjectId.getValue(7461)

        val boostedDelay = MiningService.computeMiningDelayMs(client, runite, dragonPick, boostRoll = 1)
        val normalDelay = MiningService.computeMiningDelayMs(client, runite, dragonPick, boostRoll = 2)

        assertEquals(16876L, boostedDelay)
        assertEquals(17171L, normalDelay)
    }

    @Test
    fun `random gem helper preserves invalid chance and no free slot behavior`() {
        val client = Client(EmbeddedChannel(), 1)

        assertNull(MiningService.tryAwardRandomGem(client, chance = 64, roll = 1, gemIndex = 0))
        assertNull(MiningService.tryAwardRandomGem(client, chance = 128, roll = 2, gemIndex = 0))
    }
}
