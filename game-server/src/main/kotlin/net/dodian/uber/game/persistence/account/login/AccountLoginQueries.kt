package net.dodian.uber.game.persistence.account.login

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.utilities.DbTables

internal object AccountLoginQueries {
    val webUserSelect =
        "SELECT userid, username, usergroupid, membergroupids, salt, password, pmunread FROM " +
            "${DbTables.WEB_USERS_TABLE} WHERE username = ?"

    val webUserInsert =
        "INSERT INTO ${DbTables.WEB_USERS_TABLE} (username, passworddate, birthday_search) VALUES (?, '', '')"

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
