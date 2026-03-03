package net.dodian.uber.game.persistence.v2

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Friend
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.prayer.Prayers
import net.dodian.uber.game.persistence.PlayerSaveReason

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
    val totalLevel: Int,
    val combatLevel: Int,
    val totalXp: Int,
    val skillExperience: IntArray,
    val inventory: List<ItemSlotEntry>,
    val bank: List<ItemSlotEntry>,
    val equipment: List<ItemSlotEntry>,
    val bossLog: List<NamedCountEntry>,
    val monsterLog: List<NamedCountEntry>,
    val effects: List<Int>,
    val dailyReward: List<String>,
    val prayerButtons: List<Int>,
    val boostedLevels: IntArray,
    val currentPrayer: Int,
    val lastRecover: Int,
    val friends: List<Long>,
    val currentHealth: Int,
    val fightType: Int,
    val slayerData: String,
    val essencePouch: String,
    val autocastSpellIndex: Int,
    val latestNews: Int,
    val agilityCourseStage: Int,
    val x: Int,
    val y: Int,
    val height: Int,
    val farming: String,
    val songUnlocked: String,
    val travel: String,
    val look: String,
    val unlocks: String,
    val loginDurationMs: Long,
    val segments: List<PlayerSaveSegmentSnapshot>,
) {
    companion object {
        @JvmStatic
        fun fromClient(
            client: Client,
            sequence: Long,
            reason: PlayerSaveReason,
            updateProgress: Boolean,
            finalSave: Boolean,
            dirtyMask: Int,
        ): PlayerSaveEnvelope {
            val enabledSkills = Skill.values().filter { it.isEnabled }
            val skillExperience = IntArray(enabledSkills.size)
            var totalXp = 0
            enabledSkills.forEachIndexed { index, skill ->
                val experience = client.getExperience(skill)
                skillExperience[index] = experience
                totalXp += experience
            }

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

            val inventory = collectSlots(client.playerItems.clone(), client.playerItemsN.clone()) { rawId -> rawId - 1 }
            val bank = collectSlots(client.bankItems.clone(), client.bankItemsN.clone()) { rawId -> rawId - 1 }
            val equipment = collectSlots(client.equipment.clone(), client.equipmentN.clone()) { rawId -> rawId }

            val bossLog = client.boss_name.indices.map { index ->
                NamedCountEntry(client.boss_name[index], client.boss_amount[index])
            }

            val monsterLog = client.monsterName.indices.map { index ->
                NamedCountEntry(client.monsterName[index], client.monsterCount[index])
            }

            val effects = client.effects.toList()
            val rewards = ArrayList<String>(client.dailyReward.size)
            rewards.addAll(client.dailyReward)
            val prayerButtons =
                Prayers.Prayer.values().filter { prayer -> client.prayerManager.isPrayerOn(prayer) }.map { it.buttonId }
            val boosted =
                IntArray(client.boostedLevel.size) { index -> client.boostedLevel[index] }
            val friends =
                client.friends
                    .filter { friend: Friend -> friend.name > 0 }
                    .take(200)
                    .map { friend -> friend.name }

            return PlayerSaveEnvelope(
                sequence = sequence,
                createdAt = System.currentTimeMillis(),
                dbId = client.dbId,
                playerName = client.playerName,
                reason = reason,
                updateProgress = updateProgress,
                finalSave = finalSave,
                dirtyMask = dirtyMask,
                totalLevel = client.totalLevel(),
                combatLevel = client.determineCombatLevel(),
                totalXp = totalXp,
                skillExperience = skillExperience,
                inventory = inventory,
                bank = bank,
                equipment = equipment,
                bossLog = bossLog,
                monsterLog = monsterLog,
                effects = effects,
                dailyReward = rewards,
                prayerButtons = prayerButtons,
                boostedLevels = boosted,
                currentPrayer = client.currentPrayer,
                lastRecover = client.lastRecover,
                friends = friends,
                currentHealth = client.currentHealth,
                fightType = client.fightType,
                slayerData = client.saveTaskAsString(),
                essencePouch = client.getPouches(),
                autocastSpellIndex = client.autocast_spellIndex,
                latestNews = client.latestNews,
                agilityCourseStage = client.agilityCourseStage,
                x = client.position.x,
                y = client.position.y,
                height = client.position.z,
                farming = client.farmingJson.farmingSave(),
                songUnlocked = client.songUnlockedSaveText,
                travel = client.saveTravelAsString(),
                look = client.look,
                unlocks = client.saveUnlocksAsString(),
                loginDurationMs = System.currentTimeMillis() - client.session_start,
                segments = PlayerSaveSegment.fromMask(dirtyMask).map { segment ->
                    PlayerSaveSegmentSnapshot(segment = segment)
                },
            )
        }
    }
}
