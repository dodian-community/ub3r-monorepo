package net.dodian.uber.game.persistence.account.login

import java.sql.DriverManager
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountLoginRepositoryTest {
    @Test
    fun `create mode returns username taken before password verification for case variants`() {
        DriverManager.getConnection(
            "jdbc:h2:mem:account-create-collision-${System.nanoTime()};MODE=MySQL;NON_KEYWORDS=USER",
        ).use { connection ->
            val salt = "kept-salt"
            val storedHash = Client.passHash("existing-password", salt)
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE user (
                        userid INT PRIMARY KEY,
                        username VARCHAR(32) NOT NULL UNIQUE,
                        usergroupid INT NOT NULL,
                        membergroupids VARCHAR(128) NOT NULL,
                        salt VARCHAR(64) NOT NULL,
                        password VARCHAR(64) NOT NULL,
                        pmunread INT NOT NULL,
                        email VARCHAR(128) NOT NULL
                    )
                    """.trimIndent(),
                )
            }
            connection.prepareStatement("INSERT INTO user VALUES (7, 'Existing', 3, '', ?, ?, 0, 'kept@example.com')").use { statement ->
                statement.setString(1, salt)
                statement.setString(2, storedHash)
                statement.executeUpdate()
            }

            val result = AccountLoginService.prepareGame(
                player = Client(null, 1),
                playerName = "existing",
                playerPass = "definitely-wrong",
                email = "authorized@example.com",
                discordId = "discord-1",
                discordUsername = "discord-user",
                discordVerified = true,
                accountCreationRequested = true,
                connection = connection,
                allowDevAutoCreate = false,
                allowDevPasswordBypass = false,
            )

            assertEquals(AccountLoginService.USERNAME_TAKEN, result.code)
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT password, salt, email FROM user WHERE userid = 7").use { results ->
                    assertTrue(results.next())
                    assertEquals(storedHash, results.getString("password"))
                    assertEquals(salt, results.getString("salt"))
                    assertEquals("kept@example.com", results.getString("email"))
                }
            }
        }
    }

    @Test
    fun `duplicate username insert is classified separately from discord account limit`() {
        DriverManager.getConnection(
            "jdbc:h2:mem:account-create-race-${System.nanoTime()};MODE=MySQL;NON_KEYWORDS=USER",
        ).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE user (
                        userid INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(32) NOT NULL UNIQUE,
                        usergroupid INT NOT NULL DEFAULT 3,
                        membergroupids VARCHAR(128) NOT NULL DEFAULT '',
                        salt VARCHAR(64) NOT NULL,
                        password VARCHAR(64) NOT NULL,
                        pmunread INT NOT NULL DEFAULT 0,
                        email VARCHAR(128) NOT NULL,
                        discord_id VARCHAR(32) NOT NULL DEFAULT '',
                        discord_username VARCHAR(64) NOT NULL DEFAULT '',
                        passworddate VARCHAR(64) NOT NULL DEFAULT '',
                        birthday_search VARCHAR(64) NOT NULL DEFAULT ''
                    )
                    """.trimIndent(),
                )
                statement.execute(
                    "INSERT INTO user (username, salt, password, email, discord_id) " +
                        "VALUES ('Taken', '', '', 'kept@example.com', 'discord-existing')",
                )
            }

            val result = AccountLoginRepository.insertWebUserIfUnderDiscordLimit(
                connection = connection,
                playerName = "Taken",
                playerPass = "new-password",
                email = "new@example.com",
                discordId = "discord-new",
                discordUsername = "new-user",
                maxAllowed = 3,
            )

            assertEquals(AccountLoginRepository.AccountCreationInsertResult.USERNAME_TAKEN, result)
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT COUNT(*) FROM user").use { results ->
                    assertTrue(results.next())
                    assertEquals(1, results.getInt(1))
                }
            }
        }
    }

    @Test
    fun `normal login never creates an unknown account even when dev auto-create is enabled`() {
        DriverManager.getConnection(
            "jdbc:h2:mem:account-login-mode-${System.nanoTime()};MODE=MySQL;NON_KEYWORDS=USER",
        ).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE user (
                        userid INT PRIMARY KEY,
                        username VARCHAR(32) NOT NULL,
                        usergroupid INT NOT NULL,
                        membergroupids VARCHAR(128) NOT NULL,
                        salt VARCHAR(64) NOT NULL,
                        password VARCHAR(64) NOT NULL,
                        pmunread INT NOT NULL
                    )
                    """.trimIndent(),
                )
            }

            val result = AccountLoginService.prepareGame(
                player = Client(null, 1),
                playerName = "Unknown",
                playerPass = "password",
                accountCreationRequested = false,
                connection = connection,
                allowDevAutoCreate = true,
                allowDevPasswordBypass = true,
            )

            assertEquals(26, result.code)
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT COUNT(*) FROM user").use { results ->
                    assertTrue(results.next())
                    assertEquals(0, results.getInt(1))
                }
            }
        }
    }

    @Test
    fun `passwordless login never initializes credentials`() {
        DriverManager.getConnection(
            "jdbc:h2:mem:account-login-${System.nanoTime()};MODE=MySQL;NON_KEYWORDS=USER",
        ).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE user (
                        userid INT PRIMARY KEY,
                        username VARCHAR(32) NOT NULL,
                        usergroupid INT NOT NULL,
                        membergroupids VARCHAR(128) NOT NULL,
                        salt VARCHAR(64) NOT NULL,
                        password VARCHAR(64) NOT NULL,
                        pmunread INT NOT NULL,
                        email VARCHAR(128) NOT NULL
                    )
                    """.trimIndent(),
                )
                statement.execute("INSERT INTO user VALUES (41, 'Neosback', 3, '', '', '', 0, 'kept@example.com')")
            }

            val result = AccountLoginService.prepareGame(
                player = Client(null, 1),
                playerName = "Neosback",
                playerPass = "first-password",
                connection = connection,
                allowDevAutoCreate = false,
                allowDevPasswordBypass = false,
            )

            assertEquals(3, result.code)
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT password, salt, email FROM user WHERE userid = 41").use { results ->
                    assertTrue(results.next())
                    assertEquals("", results.getString("password"))
                    assertEquals("", results.getString("salt"))
                    assertEquals("kept@example.com", results.getString("email"))
                }
            }
        }
    }

    @Test
    fun `incorrect password never rewrites existing credentials`() {
        DriverManager.getConnection(
            "jdbc:h2:mem:account-login-wrong-${System.nanoTime()};MODE=MySQL;NON_KEYWORDS=USER",
        ).use { connection ->
            val salt = "salt-one"
            val storedHash = Client.passHash("correct-password", salt)
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE user (
                        userid INT PRIMARY KEY,
                        username VARCHAR(32) NOT NULL,
                        usergroupid INT NOT NULL,
                        membergroupids VARCHAR(128) NOT NULL,
                        salt VARCHAR(16) NOT NULL,
                        password VARCHAR(64) NOT NULL,
                        pmunread INT NOT NULL,
                        email VARCHAR(128) NOT NULL
                    )
                    """.trimIndent(),
                )
            }
            connection.prepareStatement("INSERT INTO user VALUES (9, ?, 3, '', ?, ?, 0, 'kept@example.com')").use { statement ->
                statement.setString(1, "Neosback")
                statement.setString(2, salt)
                statement.setString(3, storedHash)
                statement.executeUpdate()
            }

            val result = AccountLoginService.prepareGame(
                player = Client(null, 1),
                playerName = "Neosback",
                playerPass = "wrong-password",
                connection = connection,
                allowDevAutoCreate = false,
                allowDevPasswordBypass = false,
            )

            assertEquals(3, result.code)
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT password, salt, email FROM user WHERE userid = 9").use { results ->
                    assertTrue(results.next())
                    assertEquals(storedHash, results.getString("password"))
                    assertEquals(salt, results.getString("salt"))
                    assertEquals("kept@example.com", results.getString("email"))
                }
            }
        }
    }
}
