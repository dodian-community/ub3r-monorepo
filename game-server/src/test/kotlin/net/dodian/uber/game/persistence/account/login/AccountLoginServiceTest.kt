package net.dodian.uber.game.persistence.account.login

import io.netty.channel.embedded.EmbeddedChannel
import java.sql.Connection
import java.sql.DriverManager
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.utilities.DbTables
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountLoginServiceTest {
    @Test
    fun `loadGame succeeds from joined character and stats row`() {
        withConnection { connection ->
            createSchema(connection)
            insertUser(connection, "alice", "secret")
            insertCharacter(connection, dbId = 1, name = "alice", unbanTime = 0L)
            insertStats(connection, dbId = 1)

            val client = client(1)
            val code = AccountLoginService.loadGame(client, "alice", "secret", connection, false, false)

            assertEquals(0, code)
            assertEquals(1, client.dbId)
            assertEquals(10, client.maxHealth)
            assertEquals(10, client.currentHealth)
            assertTrue(client.loadingDone)
        }
    }

    @Test
    fun `loadGame rejects banned account from joined character row`() {
        withConnection { connection ->
            createSchema(connection)
            insertUser(connection, "banned", "secret")
            insertCharacter(connection, dbId = 1, name = "banned", unbanTime = System.currentTimeMillis() + 60_000L)
            insertStats(connection, dbId = 1)

            val client = client(1)
            val code = AccountLoginService.loadGame(client, "banned", "secret", connection, false, false)

            assertEquals(4, code)
        }
    }

    @Test
    fun `missing stats row uses defaults and backfills character stats`() {
        withConnection { connection ->
            createSchema(connection)
            insertUser(connection, "nostats", "secret")
            insertCharacter(connection, dbId = 1, name = "nostats", unbanTime = 0L)

            val client = client(1)
            val code = AccountLoginService.loadGame(client, "nostats", "secret", connection, false, false)

            assertEquals(0, code)
            assertEquals(10, client.maxHealth)
            assertEquals(10, client.currentHealth)
            connection.prepareStatement("SELECT COUNT(*) FROM ${DbTables.GAME_CHARACTERS_STATS} WHERE uid = ?").use { statement ->
                statement.setInt(1, 1)
                statement.executeQuery().use { results ->
                    results.next()
                    assertEquals(1, results.getInt(1))
                }
            }
        }
    }

    @Test
    fun `missing web user denied in prod and auto-created in dev mode`() {
        withConnection { connection ->
            createSchema(connection)

            val prodClient = client(1)
            assertEquals(12, AccountLoginService.loadCharacterGame(prodClient, "newprod", "secret", connection, false, false))

            val devClient = client(2).apply { connectedFrom = "127.0.0.1" }
            assertEquals(0, AccountLoginService.loadCharacterGame(devClient, "newdev", "secret", connection, true, true))
            connection.prepareStatement("SELECT COUNT(*) FROM ${DbTables.WEB_USERS_TABLE} WHERE username = ?").use { statement ->
                statement.setString(1, "newdev")
                statement.executeQuery().use { results ->
                    results.next()
                    assertEquals(1, results.getInt(1))
                }
            }
        }
    }

    private fun client(slot: Int): Client = Client(EmbeddedChannel(), slot)

    private fun withConnection(block: (Connection) -> Unit) {
        DriverManager.getConnection(nextH2Url()).use(block)
    }

    private fun createSchema(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE "${DbTables.WEB_USERS_TABLE}" (
                    userid INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(64) NOT NULL,
                    usergroupid INT DEFAULT 40,
                    membergroupids VARCHAR(128) DEFAULT '',
                    salt VARCHAR(64) DEFAULT '',
                    password VARCHAR(128) DEFAULT '',
                    pmunread INT DEFAULT 0,
                    passworddate VARCHAR(32) DEFAULT '',
                    birthday_search VARCHAR(32) DEFAULT ''
                )
                """.trimIndent(),
            )
            statement.execute(
                """
                CREATE TABLE ${DbTables.GAME_CHARACTERS} (
                    id INT PRIMARY KEY,
                    name VARCHAR(64) NOT NULL,
                    look VARCHAR(255) DEFAULT '',
                    news INT DEFAULT 0,
                    x INT DEFAULT 3200,
                    y INT DEFAULT 3200,
                    height INT DEFAULT 0,
                    unmutetime BIGINT DEFAULT 0,
                    unbantime BIGINT DEFAULT 0,
                    fightStyle INT DEFAULT 0,
                    autocast INT DEFAULT -1,
                    health INT DEFAULT 10,
                    prayer VARCHAR(255) DEFAULT '',
                    boosted VARCHAR(255) DEFAULT '',
                    inventory VARCHAR(4000) DEFAULT '',
                    equipment VARCHAR(4000) DEFAULT '',
                    slayerData VARCHAR(255) DEFAULT '',
                    agility INT DEFAULT 0,
                    travel VARCHAR(255) DEFAULT '',
                    unlocks VARCHAR(255) DEFAULT '',
                    bank VARCHAR(4000) DEFAULT '',
                    essence_pouch VARCHAR(64) DEFAULT '0:0:0:0',
                    songUnlocked VARCHAR(255) DEFAULT '',
                    friends VARCHAR(255) DEFAULT '',
                    Boss_Log VARCHAR(255),
                    Monster_Log VARCHAR(255),
                    effects VARCHAR(255),
                    dailyReward VARCHAR(255),
                    farming VARCHAR(255),
                    lastlogin BIGINT DEFAULT 1
                )
                """.trimIndent(),
            )
            val skillColumns =
                Skill.values()
                    .filter { it.isEnabled }
                    .joinToString(",\n                    ") { "${it.name.lowercase()} INT DEFAULT 0" }
            statement.execute(
                """
                CREATE TABLE ${DbTables.GAME_CHARACTERS_STATS} (
                    uid INT PRIMARY KEY,
                    $skillColumns
                )
                """.trimIndent(),
            )
        }
    }

    private fun insertUser(connection: Connection, username: String, password: String) {
        connection.prepareStatement(
            "INSERT INTO ${DbTables.WEB_USERS_TABLE} (username, usergroupid, membergroupids, salt, password, pmunread, passworddate, birthday_search) VALUES (?, 40, '', 'salt', ?, 0, '', '')",
        ).use { statement ->
            statement.setString(1, username)
            statement.setString(2, Client.passHash(password, "salt"))
            statement.executeUpdate()
        }
    }

    private fun insertCharacter(connection: Connection, dbId: Int, name: String, unbanTime: Long) {
        connection.prepareStatement(
            """
            INSERT INTO ${DbTables.GAME_CHARACTERS}
                (id, name, look, news, x, y, height, unmutetime, unbantime, fightStyle, autocast, health, prayer, boosted, inventory, equipment, slayerData, agility, travel, unlocks, bank, essence_pouch, songUnlocked, friends, Boss_Log, Monster_Log, effects, dailyReward, farming, lastlogin)
            VALUES
                (?, ?, '', 0, 3200, 3200, 0, 0, ?, 0, -1, 10, '', '', '', '', '', 0, '', '', '', '0:0:0:0', '', '', NULL, NULL, NULL, NULL, '', 1)
            """.trimIndent(),
        ).use { statement ->
            statement.setInt(1, dbId)
            statement.setString(2, name)
            statement.setLong(3, unbanTime)
            statement.executeUpdate()
        }
    }

    private fun insertStats(connection: Connection, dbId: Int) {
        val enabledSkills = Skill.values().filter { it.isEnabled }
        val skillNames = enabledSkills.joinToString(", ") { it.name.lowercase() }
        val placeholders = enabledSkills.joinToString(", ") { "?" }
        connection.prepareStatement(
            "INSERT INTO ${DbTables.GAME_CHARACTERS_STATS} (uid, $skillNames) VALUES (?, $placeholders)",
        ).use { statement ->
            statement.setInt(1, dbId)
            enabledSkills.forEachIndexed { index, skill ->
                val xp = if (skill == Skill.HITPOINTS) 1155 else 0
                statement.setInt(index + 2, xp)
            }
            statement.executeUpdate()
        }
    }

    private fun nextH2Url(): String =
        "jdbc:h2:mem:account_login_${System.nanoTime()};MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER"
}
