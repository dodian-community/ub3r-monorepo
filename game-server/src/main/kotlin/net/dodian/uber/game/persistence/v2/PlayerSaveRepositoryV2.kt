package net.dodian.uber.game.persistence.v2

import java.sql.Connection
import java.sql.SQLException
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.persistence.PlayerSaveRepository
import net.dodian.uber.game.persistence.PlayerSaveSnapshot
import net.dodian.utilities.DbTables

class PlayerSaveRepositoryV2(
    private val delegate: PlayerSaveRepository = PlayerSaveRepository(),
) {
    fun saveEnvelope(envelope: PlayerSaveEnvelope) {
        delegate.saveSnapshot(buildSnapshot(envelope))
    }

    fun buildSnapshot(envelope: PlayerSaveEnvelope): PlayerSaveSnapshot {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(envelope.createdAt))
        val skills = Skill.values().filter { it.isEnabled }

        val statsQuery =
            buildString {
                append("UPDATE ")
                append(DbTables.GAME_CHARACTERS_STATS)
                append(" SET total=")
                append(envelope.totalLevel)
                append(", combat=")
                append(envelope.combatLevel)
                append(", ")
                skills.forEachIndexed { index, skill ->
                    append(skill.name.lowercase())
                    append("=")
                    append(envelope.skillExperience[index])
                    append(", ")
                }
                append("totalxp=")
                append(envelope.totalXp)
                append(" WHERE uid=")
                append(envelope.dbId)
            }

        val progressQuery =
            if (!envelope.updateProgress) {
                null
            } else {
                buildString {
                    append("INSERT INTO ")
                    append(DbTables.GAME_CHARACTERS_STATS_PROGRESS)
                    append(" SET updated='")
                    append(timestamp)
                    append("', total=")
                    append(envelope.totalLevel)
                    append(", combat=")
                    append(envelope.combatLevel)
                    append(", uid=")
                    append(envelope.dbId)
                    append(", ")
                    skills.forEachIndexed { index, skill ->
                        append(skill.name.lowercase())
                        append("=")
                        append(envelope.skillExperience[index])
                        append(", ")
                    }
                    append("totalxp=")
                    append(envelope.totalXp)
                }
            }

        val characterQuery =
            buildString {
                append("UPDATE ")
                append(DbTables.GAME_CHARACTERS)
                append(" SET pkrating=1500")
                append(", health=")
                append(envelope.currentHealth)
                append(", equipment='")
                append(encodeItemSlots(envelope.equipment))
                append("', inventory='")
                append(encodeItemSlots(envelope.inventory))
                append("', bank='")
                append(encodeItemSlots(envelope.bank))
                append("', friends='")
                append(envelope.friends.joinToString(" "))
                append("', fightStyle = ")
                append(envelope.fightType)
                append(", slayerData='")
                append(envelope.slayerData)
                append("', essence_pouch='")
                append(envelope.essencePouch)
                append("', effects='")
                append(envelope.effects.joinToString(":"))
                append("'")
                append(", autocast=")
                append(envelope.autocastSpellIndex)
                append(", news=")
                append(envelope.latestNews)
                append(", agility = '")
                append(envelope.agilityCourseStage)
                append("', height = ")
                append(envelope.height)
                append(", x = ")
                append(envelope.x)
                append(", y = ")
                append(envelope.y)
                append(", lastlogin = '")
                append(System.currentTimeMillis())
                append("', Monster_Log='")
                append(encodeNamedCounts(envelope.monsterLog, ",", ";"))
                append("', farming = '")
                append(envelope.farming)
                append("', dailyReward = '")
                append(encodeDailyReward(envelope.dailyReward))
                append("',Boss_Log='")
                append(encodeNamedCounts(envelope.bossLog, ":", " "))
                append("', songUnlocked='")
                append(envelope.songUnlocked)
                append("', travel='")
                append(envelope.travel)
                append("', look='")
                append(envelope.look)
                append("', unlocks='")
                append(envelope.unlocks)
                append("'")
                append(", prayer='")
                append(encodePrayer(envelope))
                append("', boosted='")
                append(encodeBoosted(envelope))
                append("'")
                if (envelope.loginDurationMs > 10_000L) {
                    append(", lastlogin = '")
                    append(System.currentTimeMillis())
                    append("'")
                }
                append(" WHERE id = ")
                append(envelope.dbId)
            }

        return PlayerSaveSnapshot.forSql(
            envelope.sequence,
            envelope.dbId,
            envelope.playerName,
            envelope.reason,
            envelope.updateProgress,
            envelope.finalSave,
            statsQuery,
            progressQuery,
            characterQuery,
        )
    }

    private fun encodeItemSlots(entries: List<ItemSlotEntry>): String =
        entries.joinToString(" ") { entry -> "${entry.slot}-${entry.itemId}-${entry.amount}" }

    private fun encodeNamedCounts(entries: List<NamedCountEntry>, kvSeparator: String, entrySeparator: String): String =
        entries.joinToString(entrySeparator) { entry -> "${entry.name}$kvSeparator${entry.count}" }

    private fun encodeDailyReward(rewards: List<String>): String {
        if (rewards.isEmpty()) {
            return ""
        }
        return rewards.joinToString(",")
    }

    private fun encodePrayer(envelope: PlayerSaveEnvelope): String {
        if (envelope.prayerButtons.isEmpty()) {
            return envelope.currentPrayer.toString()
        }
        return buildString {
            append(envelope.currentPrayer)
            envelope.prayerButtons.forEach { buttonId ->
                append(":")
                append(buttonId)
            }
        }
    }

    private fun encodeBoosted(envelope: PlayerSaveEnvelope): String =
        buildString {
            append(envelope.lastRecover)
            envelope.boostedLevels.forEach { boost ->
                append(":")
                append(boost)
            }
        }
}
