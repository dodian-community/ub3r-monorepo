package net.dodian.uber.game.persistence.account.login

import java.sql.Connection
import java.sql.ResultSet
import net.dodian.uber.game.model.player.skills.Skill

internal object AccountLoginRepository {
    data class WebUserRow(
        val dbId: Int,
        val username: String,
        val playerGroup: Int,
        val otherGroups: Array<String>,
        val salt: String,
        val password: String,
        val unreadPmCount: Int,
    )

    data class JoinedCharacterRow(
        val unbanTime: Long,
        val look: String,
        val latestNews: Int,
        val x: Int,
        val y: Int,
        val z: Int,
        val unmuteTime: Long,
        val fightStyle: Int,
        val autocast: Int,
        val health: Int,
        val prayer: String,
        val boosted: String,
        val inventory: String,
        val equipment: String,
        val slayerData: String,
        val agilityStage: Int,
        val travel: String,
        val unlocks: String,
        val bank: String,
        val essencePouch: String,
        val songUnlocked: String,
        val friends: String,
        val bossLog: String?,
        val monsterLog: String?,
        val effects: String?,
        val dailyReward: String?,
        val farming: String?,
        val lastLogin: Long,
        val statsPresent: Boolean,
        val skillExperience: Map<Skill, Int>,
    )

    fun loadWebUser(connection: Connection, playerName: String): WebUserRow? =
        connection.prepareStatement(AccountLoginQueries.webUserSelect).use { statement ->
            statement.setString(1, playerName)
            statement.executeQuery().use { results ->
                if (!results.next()) {
                    return null
                }
                WebUserRow(
                    dbId = results.getInt("userid"),
                    username = trimToEmpty(results.getString("username")),
                    playerGroup = results.getInt("usergroupid"),
                    otherGroups = splitCsvOrEmpty(results.getString("membergroupids")),
                    salt = trimToEmpty(results.getString("salt")),
                    password = trimToEmpty(results.getString("password")),
                    unreadPmCount = results.getInt("pmunread"),
                )
            }
        }

    fun insertWebUser(connection: Connection, playerName: String) {
        connection.prepareStatement(AccountLoginQueries.webUserInsert).use { statement ->
            statement.setString(1, playerName)
            statement.executeUpdate()
        }
    }

    fun loadCharacter(connection: Connection, dbId: Int): JoinedCharacterRow? =
        connection.prepareStatement(AccountLoginQueries.characterLoadSelect).use { statement ->
            statement.setInt(1, dbId)
            statement.executeQuery().use { results ->
                if (!results.next()) {
                    return null
                }
                JoinedCharacterRow(
                    unbanTime = results.getLong("unbantime"),
                    look = trimToEmpty(results.getString("look")),
                    latestNews = results.getInt("news"),
                    x = results.getInt("x"),
                    y = results.getInt("y"),
                    z = results.getInt("height"),
                    unmuteTime = results.getLong("unmutetime"),
                    fightStyle = results.getInt("fightStyle"),
                    autocast = results.getInt("autocast"),
                    health = results.getInt("health"),
                    prayer = trimToEmpty(results.getString("prayer")),
                    boosted = trimToEmpty(results.getString("boosted")),
                    inventory = trimToEmpty(results.getString("inventory")),
                    equipment = trimToEmpty(results.getString("equipment")),
                    slayerData = trimToEmpty(results.getString("slayerData")),
                    agilityStage = results.getInt("agility"),
                    travel = trimToEmpty(results.getString("travel")),
                    unlocks = trimToEmpty(results.getString("unlocks")),
                    bank = trimToEmpty(results.getString("bank")),
                    essencePouch = trimToEmpty(results.getString("essence_pouch")),
                    songUnlocked = trimToEmpty(results.getString("songUnlocked")),
                    friends = trimToEmpty(results.getString("friends")),
                    bossLog = results.getString("Boss_Log"),
                    monsterLog = results.getString("Monster_Log"),
                    effects = results.getString("effects"),
                    dailyReward = results.getString("dailyReward"),
                    farming = results.getString("farming"),
                    lastLogin = results.getLong("lastlogin"),
                    statsPresent = results.getObject("stats_uid") != null,
                    skillExperience = readSkillExperience(results),
                )
            }
        }

    fun backfillMissingStats(connection: Connection, dbId: Int) {
        connection.prepareStatement(AccountLoginQueries.statsBackfillInsert).use { statement ->
            statement.setInt(1, dbId)
            statement.executeUpdate()
        }
    }

    fun createCharacter(connection: Connection, dbId: Int, playerName: String) {
        connection.prepareStatement(AccountLoginQueries.characterCreateInsert).use { characterInsert ->
            characterInsert.setInt(1, dbId)
            characterInsert.setString(2, playerName)
            characterInsert.executeUpdate()
        }
        backfillMissingStats(connection, dbId)
    }

    fun updateForumRegistration(connection: Connection, dbId: Int, userGroupId: String) {
        connection.prepareStatement(AccountLoginQueries.updateForumRegistration).use { statement ->
            statement.setString(1, userGroupId)
            statement.setInt(2, dbId)
            statement.executeUpdate()
        }
    }

    fun isBanned(connection: Connection, dbId: Int): Boolean =
        connection.prepareStatement(AccountLoginQueries.banStatusSelect).use { statement ->
            statement.setInt(1, dbId)
            statement.executeQuery().use { results ->
                results.next() && System.currentTimeMillis() < results.getLong("unbantime")
            }
        }

    private fun readSkillExperience(results: ResultSet): Map<Skill, Int> {
        val values = LinkedHashMap<Skill, Int>()
        for (skill in Skill.enabledSkills()) {
            values[skill] = results.getInt("stat_${skill.name}")
        }
        return values
    }

    private fun trimToEmpty(value: String?): String = value?.trim().orEmpty()

    private fun splitCsvOrEmpty(value: String?): Array<String> {
        val trimmed = trimToEmpty(value)
        return if (trimmed.isEmpty()) emptyArray() else trimmed.split(",").toTypedArray()
    }
}
