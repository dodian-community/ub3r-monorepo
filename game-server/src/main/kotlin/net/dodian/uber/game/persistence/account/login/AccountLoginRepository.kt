package net.dodian.uber.game.persistence.account.login

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.persistence.account.AccountPasswordRepository

object AccountLoginRepository {
    enum class AccountCreationInsertResult {
        INSERTED,
        DISCORD_LIMIT_REACHED,
        USERNAME_TAKEN,
    }

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
        val questData: String,
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

    fun insertWebUser(
        connection: Connection,
        playerName: String,
        playerPass: String = "",
        email: String = "",
        discordId: String = "",
        discordUsername: String = "",
    ) {
        val credential = playerPass.takeIf { it.isNotEmpty() }?.let(AccountPasswordRepository::createCredential)
        val salt = credential?.salt.orEmpty()
        val hashedPassword = credential?.hash.orEmpty()
        connection.prepareStatement(AccountLoginQueries.webUserInsert).use { statement ->
            statement.setString(1, playerName)
            statement.setString(2, hashedPassword)
            statement.setString(3, salt)
            statement.setString(4, email)
            statement.setString(5, discordId)
            statement.setString(6, discordUsername)
            statement.executeUpdate()
        }
    }

    /**
     * Atomically checks the per-Discord-account limit and inserts under a row lock, so two concurrent
     * creation attempts for the same discordId can't both pass the count check before either commits.
     * Returns false (and inserts nothing) if [maxAllowed] is already reached.
     */
    fun insertWebUserIfUnderDiscordLimit(
        connection: Connection,
        playerName: String,
        playerPass: String,
        email: String,
        discordId: String,
        discordUsername: String,
        maxAllowed: Int,
    ): AccountCreationInsertResult {
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            val count = connection.prepareStatement(AccountLoginQueries.accountsByDiscordIdForUpdate).use { statement ->
                statement.setString(1, discordId)
                statement.executeQuery().use { results ->
                    var total = 0
                    while (results.next()) total++
                    total
                }
            }
            if (count >= maxAllowed) {
                connection.rollback()
                return AccountCreationInsertResult.DISCORD_LIMIT_REACHED
            }
            insertWebUser(connection, playerName, playerPass, email, discordId, discordUsername)
            connection.commit()
            return AccountCreationInsertResult.INSERTED
        } catch (exception: SQLException) {
            connection.rollback()
            if (isUniqueConstraintViolation(exception) && loadWebUser(connection, playerName) != null) {
                return AccountCreationInsertResult.USERNAME_TAKEN
            }
            throw exception
        } catch (exception: Exception) {
            connection.rollback()
            throw exception
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }

    private fun isUniqueConstraintViolation(exception: SQLException): Boolean {
        var current: SQLException? = exception
        while (current != null) {
            if (current.errorCode == 1062 ||
                current.sqlState == "23000" ||
                current.sqlState == "23505"
            ) {
                return true
            }
            current = current.nextException
        }
        return false
    }

    fun countAccountsByDiscordId(connection: Connection, discordId: String): Int {
        if (discordId.isBlank()) return 0
        connection.prepareStatement(AccountLoginQueries.countAccountsByDiscordId).use { statement ->
            statement.setString(1, discordId)
            statement.executeQuery().use { results ->
                return if (results.next()) results.getInt("total") else 0
            }
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
                    questData = trimToEmpty(results.getString("quest_data")),
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
