package net.dodian.uber.game.persistence.player

import java.text.SimpleDateFormat
import java.util.Date
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Friend
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.prayer.Prayers
import net.dodian.uber.game.persistence.db.DbTables

data class PlayerSaveSnapshot(
    val sequence: Long,
    val createdAt: Long,
    val dbId: Int,
    val playerName: String,
    val reason: PlayerSaveReason,
    val updateProgress: Boolean,
    val finalSave: Boolean,
    val statsUpdateSql: String,
    val statsProgressInsertSql: String?,
    val characterUpdateSql: String,
) {
    companion object {
        @JvmStatic
        fun fromClient(
            client: Client,
            sequence: Long,
            reason: PlayerSaveReason,
            updateProgress: Boolean,
            finalSave: Boolean,
        ): PlayerSaveSnapshot {
            var allXp = 0L
            for (skill in Skill.enabledSkills()) {
                allXp += client.getExperience(skill)
            }

            val totalLevel = client.totalLevel()
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            val combatLevel = client.determineCombatLevel()
            val statsQuery = StringBuilder(
                "UPDATE ${DbTables.GAME_CHARACTERS_STATS} SET total=$totalLevel, combat=$combatLevel, ",
            )
            val progressQuery = StringBuilder(
                "INSERT INTO ${DbTables.GAME_CHARACTERS_STATS_PROGRESS} SET updated='$timeStamp', total=$totalLevel, combat=$combatLevel, uid=${client.dbId}, ",
            )

            for (skill in Skill.enabledSkills()) {
                statsQuery.append(skill.name).append('=').append(client.getExperience(skill)).append(", ")
                progressQuery.append(skill.name).append('=').append(client.getExperience(skill)).append(", ")
            }
            statsQuery.append("totalxp=").append(allXp).append(" WHERE uid=").append(client.dbId)
            progressQuery.append("totalxp=").append(allXp)

            val inventory = StringBuilder()
            val equipment = StringBuilder()
            val bank = StringBuilder()
            val list = StringBuilder()
            val bossLog = StringBuilder()
            val monsterLog = StringBuilder()
            val effect = StringBuilder()
            val dailyReward = StringBuilder()
            val prayer = StringBuilder()
            val boosted = StringBuilder()

            for (i in client.playerItems.indices) {
                if (client.playerItems[i] > 0) {
                    inventory.append(i).append('-').append(client.playerItems[i] - 1).append('-').append(client.playerItemsN[i]).append(' ')
                }
            }
            for (i in client.bankItems.indices) {
                if (client.bankItems[i] > 0) {
                    bank.append(i).append('-').append(client.bankItems[i] - 1).append('-').append(client.bankItemsN[i]).append(' ')
                }
            }
            for (i in client.equipment.indices) {
                if (client.equipment[i] > 0) {
                    equipment.append(i).append('-').append(client.equipment[i]).append('-').append(client.equipmentN[i]).append(' ')
                }
            }
            for (i in client.boss_name.indices) {
                if (client.boss_amount[i] >= 0) {
                    bossLog.append(client.boss_name[i]).append(':').append(client.boss_amount[i]).append(' ')
                }
            }
            for (i in 0 until client.effects.size) {
                effect.append(client.effects[i]).append(if (i == client.effects.size - 1) "" else ":")
            }
            for (i in 0 until client.monsterName.size) {
                monsterLog.append(client.monsterName[i]).append(',').append(client.monsterCount[i]).append(if (i == client.monsterName.size - 1) "" else ";")
            }
            for (i in 0 until client.staffSize) {
                dailyReward.append(client.dailyReward[i]).append(
                    if (i == client.staffSize - 1 && client.dailyReward.size <= client.staffSize) "" else if (i == client.staffSize - 1) ";" else ",",
                )
            }

            prayer.append(client.currentPrayer)
            for (pray in Prayers.Prayer.values()) {
                if (client.prayerManager.isPrayerOn(pray)) {
                    prayer.append(':').append(pray.buttonId)
                }
            }
            boosted.append(client.lastRecover)
            for (boost in client.boostedLevel.clone()) {
                boosted.append(':').append(boost)
            }

            var num = 0
            for (friend: Friend in client.friends) {
                if (friend.name > 0 && num < 200) {
                    list.append(friend.name).append(' ')
                    num++
                }
            }

            var last = ""
            val elapsed = System.currentTimeMillis() - client.session_start
            if (elapsed > 10000L) {
                last = ", lastlogin = '${System.currentTimeMillis()}'"
            }

            val characterQuery =
                "UPDATE ${DbTables.GAME_CHARACTERS}" +
                    " SET pkrating=1500" +
                    ", health=${client.currentHealth}" +
                    ", equipment='$equipment', inventory='$inventory', bank='$bank'" +
                    ", friends='$list', fightStyle = ${client.fightType}" +
                    ", slayerData='${client.saveTaskAsString()}', essence_pouch='${client.getPouches()}'" +
                    ", effects='$effect'" +
                    ", autocast=${client.autocast_spellIndex}" +
                    ", news=${client.latestNews}" +
                    ", agility = '${client.agilityCourseStage}', height = ${client.position.z}" +
                    ", x = ${client.position.x}" +
                    ", y = ${client.position.y}" +
                    ", lastlogin = '${System.currentTimeMillis()}', Monster_Log='$monsterLog'" +
                    ", farming = '${client.farmingJson.farmingSaveSnapshot()}', dailyReward = '$dailyReward'" +
                    ",Boss_Log='$bossLog'" +
                    ", songUnlocked='${client.songUnlockedSaveText}'" +
                    ", travel='${client.saveTravelAsString()}'" +
                    ", look='${client.look}'" +
                    ", unlocks='${client.saveUnlocksAsString()}'" +
                    ", prayer='$prayer', boosted='$boosted'$last" +
                    " WHERE id = ${client.dbId}"

            return PlayerSaveSnapshot(
                sequence = sequence,
                createdAt = System.currentTimeMillis(),
                dbId = client.dbId,
                playerName = client.playerName,
                reason = reason,
                updateProgress = updateProgress,
                finalSave = finalSave,
                statsUpdateSql = statsQuery.toString(),
                statsProgressInsertSql = if (updateProgress) progressQuery.toString() else null,
                characterUpdateSql = characterQuery,
            )
        }

        @JvmStatic
        fun forSql(
            sequence: Long,
            dbId: Int,
            playerName: String,
            reason: PlayerSaveReason,
            updateProgress: Boolean,
            finalSave: Boolean,
            statsUpdateSql: String,
            statsProgressInsertSql: String?,
            characterUpdateSql: String,
        ): PlayerSaveSnapshot =
            PlayerSaveSnapshot(
                sequence = sequence,
                createdAt = System.currentTimeMillis(),
                dbId = dbId,
                playerName = playerName,
                reason = reason,
                updateProgress = updateProgress,
                finalSave = finalSave,
                statsUpdateSql = statsUpdateSql,
                statsProgressInsertSql = statsProgressInsertSql,
                characterUpdateSql = characterUpdateSql,
            )
    }
}
