package net.dodian.uber.game.persistence.account.login

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.persistence.db.DbTables

internal object AccountLoginQueries {
    val webUserSelect =
        "SELECT userid, username, usergroupid, membergroupids, salt, password, pmunread FROM " +
            "${DbTables.WEB_USERS_TABLE} WHERE LOWER(username) = LOWER(?)"

    val webUserInsert =
        "INSERT INTO ${DbTables.WEB_USERS_TABLE} (username, password, salt, email, discord_id, discord_username, passworddate, birthday_search) VALUES (?, ?, ?, ?, ?, ?, '', '')"

    val countAccountsByDiscordId =
        "SELECT COUNT(*) AS total FROM ${DbTables.WEB_USERS_TABLE} WHERE discord_id = ? AND discord_id != ''"

    val accountsByDiscordIdForUpdate =
        "SELECT userid FROM ${DbTables.WEB_USERS_TABLE} WHERE discord_id = ? AND discord_id != '' FOR UPDATE"

    val statsBackfillInsert =
        "INSERT INTO ${DbTables.GAME_CHARACTERS_STATS} (uid) VALUES (?) ON DUPLICATE KEY UPDATE uid = uid"

    val characterCreateInsert =
        "INSERT INTO ${DbTables.GAME_CHARACTERS} " +
            "(id, name, equipment, inventory, bank, friends, songUnlocked) VALUES (?, ?, '', '', '', '', '0')"

    val updateForumRegistration =
        "UPDATE ${DbTables.WEB_USERS_TABLE} SET usergroupid=? WHERE userid = ?"

    val banStatusSelect =
        "SELECT unbantime FROM ${DbTables.GAME_CHARACTERS} WHERE id = ?"

    val characterLoadSelect: String = buildCharacterLoadSelect()

    private fun buildCharacterLoadSelect(): String {
        val query = StringBuilder()
        query.append("SELECT c.*, s.uid AS stats_uid")
        for (skill in Skill.enabledSkills()) {
            query.append(", s.")
            query.append(skill.name)
            query.append(" AS stat_")
            query.append(skill.name)
        }
        query.append(" FROM ")
        query.append(DbTables.GAME_CHARACTERS)
        query.append(" c LEFT JOIN ")
        query.append(DbTables.GAME_CHARACTERS_STATS)
        query.append(" s ON s.uid = c.id WHERE c.id = ?")
        return query.toString()
    }
}
