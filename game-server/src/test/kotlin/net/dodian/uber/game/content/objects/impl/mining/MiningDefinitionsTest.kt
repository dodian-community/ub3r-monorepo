package net.dodian.uber.game.skills.mining

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MiningDefinitionsTest {

    @Test
    fun `every standard rock object id resolves uniquely`() {
        assertEquals(17, MiningData.rockByObjectId.size)
        MiningData.rocks.forEach { rock ->
            rock.objectIds.forEach { objectId ->
                assertEquals(rock, MiningData.rockByObjectId[objectId])
            }
        }
    }

    @Test
    fun `legacy rock rows are represented in definitions`() {
        assertEquals(9, MiningData.rocks.size)
        assertNotNull(MiningData.rockByObjectId[7471])
        assertNotNull(MiningData.rockByObjectId[7494])
        assertTrue(MiningData.rocks.any { it.oreItemId == 1436 && !it.randomGemEligible && it.restThreshold == 14 })
    }

    @Test
    fun `pickaxes are ordered from strongest to weakest`() {
        val ids = MiningData.pickaxesDescending.map { it.itemId }
        assertEquals(listOf(20014, 11920, 1275, 1271, 1273, 12297, 1269, 1267, 1265), ids)
        assertEquals(MiningData.pickaxesDescending.size, MiningData.pickaxeByItemId.size)
    }
}
