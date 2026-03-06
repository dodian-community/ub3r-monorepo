package net.dodian.uber.game.skills.mining

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class MiningDataDslParityTest {
    @Test
    fun `rock definitions preserve lookup parity`() {
        val expected =
            listOf(
                rock("Rune essence", intArrayOf(7471), 1, 1000L, 1436, 50, false, 14),
                rock("Copper", intArrayOf(7451, 7484), 1, 2000L, 436, 110, true, 4),
                rock("Tin", intArrayOf(7452, 7485), 1, 2000L, 438, 110, true, 4),
                rock("Iron", intArrayOf(7455, 7488), 15, 3000L, 440, 280, true, 4),
                rock("Coal", intArrayOf(7456, 7489), 30, 5000L, 453, 420, true, 4),
                rock("Gold", intArrayOf(7458, 7491), 40, 6000L, 444, 510, true, 4),
                rock("Mithril", intArrayOf(7459, 7492), 55, 7000L, 447, 620, true, 4),
                rock("Adamantite", intArrayOf(7460, 7493), 70, 9000L, 449, 780, true, 4),
                rock("Runite", intArrayOf(7461, 7494), 85, 35000L, 451, 3100, true, 4),
            )

        assertEquals(expected.size, MiningData.rocks.size)
        expected.zip(MiningData.rocks).forEach { (expectedRock, actualRock) ->
            assertEquals(expectedRock.name, actualRock.name)
            assertArrayEquals(expectedRock.objectIds, actualRock.objectIds)
            assertEquals(expectedRock.requiredLevel, actualRock.requiredLevel)
            assertEquals(expectedRock.baseDelayMs, actualRock.baseDelayMs)
            assertEquals(expectedRock.oreItemId, actualRock.oreItemId)
            assertEquals(expectedRock.experience, actualRock.experience)
            assertEquals(expectedRock.randomGemEligible, actualRock.randomGemEligible)
            assertEquals(expectedRock.restThreshold, actualRock.restThreshold)
        }

        val expectedObjectIds = expected.flatMap { it.objectIds.asIterable() }.sorted().toIntArray()
        assertArrayEquals(expectedObjectIds, MiningData.allRockObjectIds)
        expected.forEach { rock ->
            rock.objectIds.forEach { objectId ->
                val resolved = MiningData.rockByObjectId[objectId]
                assertNotNull(resolved)
                assertEquals(rock.name, resolved?.name)
            }
        }
        assertFalse(MiningData.rockByObjectId.containsKey(7464))
        assertFalse(MiningData.rockByObjectId.containsKey(7463))
    }

    @Test
    fun `pickaxe definitions preserve ordering and stats`() {
        val expected =
            listOf(
                pickaxe("3rd age", 20014, 61, 0.8, 7139, true),
                pickaxe("Dragon", 11920, 61, 0.8, 7139, true),
                pickaxe("Rune", 1275, 41, 0.42, 624, false),
                pickaxe("Iron", 1271, 31, 0.33, 628, false),
                pickaxe("Steel", 1273, 21, 0.24, 629, false),
                pickaxe("Black", 12297, 11, 0.15, 629, false),
                pickaxe("Mithril", 1269, 6, 0.1, 627, false),
                pickaxe("Adamant", 1267, 1, 0.065, 626, false),
                pickaxe("Bronze", 1265, 1, 0.04, 625, false),
            )

        assertEquals(expected.size, MiningData.pickaxesDescending.size)
        expected.zip(MiningData.pickaxesDescending).forEach { (expectedPickaxe, actualPickaxe) ->
            assertEquals(expectedPickaxe, actualPickaxe)
            assertEquals(actualPickaxe, MiningData.pickaxeByItemId[actualPickaxe.itemId])
        }
    }

    private fun rock(
        name: String,
        objectIds: IntArray,
        requiredLevel: Int,
        baseDelayMs: Long,
        oreItemId: Int,
        experience: Int,
        randomGemEligible: Boolean,
        restThreshold: Int,
    ) = MiningRockDef(
        name = name,
        objectIds = objectIds,
        requiredLevel = requiredLevel,
        baseDelayMs = baseDelayMs,
        oreItemId = oreItemId,
        experience = experience,
        randomGemEligible = randomGemEligible,
        restThreshold = restThreshold,
    )

    private fun pickaxe(
        name: String,
        itemId: Int,
        requiredLevel: Int,
        speedBonus: Double,
        animationId: Int,
        dragonTierBoostEligible: Boolean,
    ) = PickaxeDef(
        name = name,
        itemId = itemId,
        requiredLevel = requiredLevel,
        speedBonus = speedBonus,
        animationId = animationId,
        dragonTierBoostEligible = dragonTierBoostEligible,
    )
}
