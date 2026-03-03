package net.dodian.uber.game.persistence.v2

import net.dodian.uber.game.persistence.PlayerSaveReason
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerSaveRepositoryV2Test {
    private val repository = PlayerSaveRepositoryV2()

    @Test
    fun `buildSnapshot writes expected key fields`() {
        val envelope =
            PlayerSaveEnvelope(
                sequence = 42L,
                createdAt = 1_700_000_000_000L,
                dbId = 77,
                playerName = "tester",
                reason = PlayerSaveReason.PERIODIC,
                updateProgress = true,
                finalSave = false,
                dirtyMask = PlayerSaveSegment.ALL_MASK,
                totalLevel = 1234,
                combatLevel = 99,
                totalXp = 999_999,
                skillExperience = IntArray(21) { it * 1000 },
                inventory = listOf(ItemSlotEntry(slot = 0, itemId = 4151, amount = 1)),
                bank = listOf(ItemSlotEntry(slot = 0, itemId = 995, amount = 1_000_000)),
                equipment = listOf(ItemSlotEntry(slot = 3, itemId = 4151, amount = 1)),
                bossLog = listOf(NamedCountEntry("Dad", 3)),
                monsterLog = listOf(NamedCountEntry("Goblin", 12)),
                effects = listOf(-1, 0, 5),
                dailyReward = listOf("0", "6000", "20"),
                prayerButtons = listOf(21233, 21234),
                boostedLevels = IntArray(21),
                currentPrayer = 70,
                lastRecover = 4,
                friends = listOf(1234L, 5678L),
                currentHealth = 99,
                fightType = 1,
                slayerData = "task",
                essencePouch = "pouch",
                autocastSpellIndex = 7,
                latestNews = 5,
                agilityCourseStage = 2,
                x = 3200,
                y = 3201,
                height = 0,
                farming = "{}",
                songUnlocked = "1:0:1",
                travel = "travel",
                look = "look",
                unlocks = "unlocks",
                loginDurationMs = 20_000L,
                segments = listOf(PlayerSaveSegmentSnapshot(PlayerSaveSegment.INVENTORY)),
            )

        val snapshot = repository.buildSnapshot(envelope)

        assertEquals(42L, snapshot.sequence)
        assertTrue(snapshot.statsUpdateSql.contains("total=1234"))
        assertTrue(snapshot.statsProgressInsertSql!!.contains("uid=77"))
        assertTrue(snapshot.characterUpdateSql.contains("inventory='0-4151-1'"))
        assertTrue(snapshot.characterUpdateSql.contains("bank='0-995-1000000'"))
        assertTrue(snapshot.characterUpdateSql.contains("equipment='3-4151-1'"))
    }
}
