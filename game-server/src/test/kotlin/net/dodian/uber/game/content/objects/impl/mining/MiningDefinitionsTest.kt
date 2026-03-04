package net.dodian.uber.game.content.objects.impl.mining

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MiningDefinitionsTest {

    @Test
    fun `every standard rock object id resolves uniquely`() {
        assertEquals(MiningDefinitions.standardRocks.size, MiningDefinitions.rocksByObjectId.size)
        MiningDefinitions.standardRocks.forEach { rock ->
            assertEquals(rock, MiningDefinitions.rocksByObjectId[rock.objectId])
        }
    }

    @Test
    fun `legacy rock rows are represented in definitions`() {
        assertEquals(17, MiningDefinitions.standardRocks.size)
        assertNotNull(MiningDefinitions.rocksByObjectId[7471])
        assertNotNull(MiningDefinitions.rocksByObjectId[7494])
        assertTrue(MiningDefinitions.standardRocks.any { it.oreItemId == 1436 && !it.randomGemEligible && it.restThreshold == 14 })
    }

    @Test
    fun `pickaxes are ordered from strongest to weakest`() {
        val ids = MiningDefinitions.pickaxesDescending.map { it.itemId }
        assertEquals(listOf(20014, 11920, 1275, 1271, 1273, 12297, 1269, 1267, 1265), ids)
        assertEquals(MiningDefinitions.pickaxesDescending.size, MiningDefinitions.pickaxeByItemId.size)
    }
}
