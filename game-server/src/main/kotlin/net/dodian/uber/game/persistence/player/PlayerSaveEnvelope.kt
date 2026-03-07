package net.dodian.uber.game.persistence.player

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Friend
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.prayer.Prayers
import net.dodian.uber.game.persistence.player.PlayerSaveReason

data class ItemSlotEntry(
    val slot: Int,
    val itemId: Int,
    val amount: Int,
)

data class NamedCountEntry(
    val name: String,
    val count: Int,
)

data class PlayerSaveEnvelope(
    val sequence: Long,
    val createdAt: Long,
    val dbId: Int,
    val playerName: String,
    val reason: PlayerSaveReason,
    val updateProgress: Boolean,
    val finalSave: Boolean,
    val dirtyMask: Int,
    val saveRevisionAtCapture: Long,
    val segments: List<PlayerSaveSegmentSnapshot>,
) {
    companion object {
        private val ENABLED_SKILLS: List<Skill> = Skill.values().filter { it.isEnabled }

        @JvmStatic
        fun fromClient(
            client: Client,
            sequence: Long,
            reason: PlayerSaveReason,
            updateProgress: Boolean,
            finalSave: Boolean,
            dirtyMask: Int,
        ): PlayerSaveEnvelope {
            fun has(segment: PlayerSaveSegment): Boolean = dirtyMask and segment.mask != 0

            fun collectSlots(ids: IntArray, amounts: IntArray, transform: (Int) -> Int): List<ItemSlotEntry> {
                val entries = ArrayList<ItemSlotEntry>(ids.size)
                for (slot in ids.indices) {
                    val rawId = ids[slot]
                    if (rawId <= 0) {
                        continue
                    }
                    entries += ItemSlotEntry(slot = slot, itemId = transform(rawId), amount = amounts[slot])
                }
                return entries
            }

            val segments = ArrayList<PlayerSaveSegmentSnapshot>(8)
            val skillExperience = IntArray(ENABLED_SKILLS.size)
            var totalXp = 0
            ENABLED_SKILLS.forEachIndexed { index, skill ->
                val experience = client.getExperience(skill)
                skillExperience[index] = experience
                totalXp += experience
            }
            val prayerButtons =
                Prayers.Prayer.values()
                    .filter { prayer -> client.prayerManager.isPrayerOn(prayer) }
                    .map { it.buttonId }
                    .toIntArray()
            val boosted = IntArray(client.boostedLevel.size) { index -> client.boostedLevel[index] }
            segments +=
                StatsSegmentSnapshot(
                    totalLevel = client.totalLevel(),
                    combatLevel = client.determineCombatLevel(),
                    totalXp = totalXp,
                    skillExperience = skillExperience,
                    currentHealth = client.currentHealth,
                    currentPrayer = client.currentPrayer,
                    prayerButtons = prayerButtons,
                    boostedLevels = boosted,
                    lastRecover = client.lastRecover,
                    fightType = client.fightType,
                )

            if (has(PlayerSaveSegment.INVENTORY)) {
                segments +=
                    InventorySegmentSnapshot(
                        collectSlots(client.playerItems.clone(), client.playerItemsN.clone()) { rawId -> rawId - 1 },
                    )
            }
            if (has(PlayerSaveSegment.BANK)) {
                segments +=
                    BankSegmentSnapshot(
                        collectSlots(client.bankItems.clone(), client.bankItemsN.clone()) { rawId -> rawId - 1 },
                    )
            }
            if (has(PlayerSaveSegment.EQUIPMENT)) {
                segments +=
                    EquipmentSegmentSnapshot(
                        collectSlots(client.equipment.clone(), client.equipmentN.clone()) { rawId -> rawId },
                    )
            }
            if (has(PlayerSaveSegment.POSITION)) {
                segments += PositionSegmentSnapshot(client.position.x, client.position.y, client.position.z)
            }
            if (has(PlayerSaveSegment.SOCIAL)) {
                val friends =
                    client.friends
                        .filter { friend: Friend -> friend.name > 0 }
                        .take(200)
                        .map { friend -> friend.name }
                segments += SocialSegmentSnapshot(friends)
            }
            if (has(PlayerSaveSegment.SLAYER)) {
                segments +=
                    SlayerSegmentSnapshot(
                        slayerData = client.saveTaskAsString(),
                        essencePouch = client.getPouches(),
                        autocastSpellIndex = client.autocast_spellIndex,
                    )
            }
            if (has(PlayerSaveSegment.FARMING)) {
                val rewards = ArrayList<String>(client.dailyReward.size)
                rewards.addAll(client.dailyReward)
                segments += FarmingSegmentSnapshot(farming = client.farmingJson.farmingSaveSnapshot(), dailyReward = rewards)
            }
            if (has(PlayerSaveSegment.EFFECTS)) {
                segments += EffectsSegmentSnapshot(client.effects.toList())
            }
            if (has(PlayerSaveSegment.LOOKS)) {
                segments +=
                    LooksSegmentSnapshot(
                        songUnlocked = client.songUnlockedSaveText,
                        travel = client.saveTravelAsString(),
                        look = client.look,
                        unlocks = client.saveUnlocksAsString(),
                    )
            }
            if (has(PlayerSaveSegment.META)) {
                val bossLog =
                    client.boss_name.indices.map { index -> NamedCountEntry(client.boss_name[index], client.boss_amount[index]) }
                val monsterLog =
                    client.monsterName.indices.map { index ->
                        NamedCountEntry(client.monsterName[index], client.monsterCount[index])
                    }
                segments +=
                    MetaSegmentSnapshot(
                        latestNews = client.latestNews,
                        agilityCourseStage = client.agilityCourseStage,
                        bossLog = bossLog,
                        monsterLog = monsterLog,
                        loginDurationMs = System.currentTimeMillis() - client.session_start,
                    )
            }

            return PlayerSaveEnvelope(
                sequence = sequence,
                createdAt = System.currentTimeMillis(),
                dbId = client.dbId,
                playerName = client.playerName,
                reason = reason,
                updateProgress = updateProgress,
                finalSave = finalSave,
                dirtyMask = dirtyMask,
                saveRevisionAtCapture = client.saveRevision,
                segments = segments,
            )
        }
    }
}
