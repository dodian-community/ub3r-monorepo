package net.dodian.uber.game.persistence.account

import java.sql.DriverManager
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountPasswordRepositoryTest {
    @Test
    fun `password update is owned by the authenticated db id and rotates salt`() {
        database().use { connection ->
            val firstSalt = "first-salt"
            val secondSalt = "second-salt"
            val firstHash = Client.passHash("current-one", firstSalt)
            val secondHash = Client.passHash("current-two", secondSalt)
            connection.prepareStatement("INSERT INTO user VALUES (?, ?, ?, CURRENT_TIMESTAMP)").use { statement ->
                statement.setInt(1, 41)
                statement.setString(2, firstHash)
                statement.setString(3, firstSalt)
                statement.executeUpdate()
                statement.setInt(1, 42)
                statement.setString(2, secondHash)
                statement.setString(3, secondSalt)
                statement.executeUpdate()
            }

            val verified = AccountPasswordRepository.verifyCurrentPassword(connection, 41, "current-one")
            requireNotNull(verified)
            assertEquals(
                AccountPasswordRepository.UpdateResult.UPDATED,
                AccountPasswordRepository.updatePassword(connection, 41, verified.storedHash, "N3w! Case?£"),
            )

            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT password, salt FROM user WHERE userid = 41").use { results ->
                    assertTrue(results.next())
                    val rotatedSalt = results.getString("salt")
                    assertEquals(30, rotatedSalt.length)
                    assertNotEquals(firstSalt, rotatedSalt)
                    assertEquals(Client.passHash("N3w! Case?£", rotatedSalt), results.getString("password"))
                }
                statement.executeQuery("SELECT password, salt FROM user WHERE userid = 42").use { results ->
                    assertTrue(results.next())
                    assertEquals(secondHash, results.getString("password"))
                    assertEquals(secondSalt, results.getString("salt"))
                }
            }
        }
    }

    @Test
    fun `incorrect current password and stale authorization cannot update`() {
        database().use { connection ->
            val salt = "first-salt"
            val storedHash = Client.passHash("current", salt)
            connection.prepareStatement("INSERT INTO user VALUES (41, ?, ?, CURRENT_TIMESTAMP)").use { statement ->
                statement.setString(1, storedHash)
                statement.setString(2, salt)
                statement.executeUpdate()
            }

            assertNull(AccountPasswordRepository.verifyCurrentPassword(connection, 41, "wrong"))
            assertEquals(
                AccountPasswordRepository.UpdateResult.STALE,
                AccountPasswordRepository.updatePassword(connection, 41, "stale-hash", "new"),
            )
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT password, salt FROM user WHERE userid = 41").use { results ->
                    assertTrue(results.next())
                    assertEquals(storedHash, results.getString("password"))
                    assertEquals(salt, results.getString("salt"))
                }
            }
        }
    }

    private fun database() = DriverManager.getConnection(
        "jdbc:h2:mem:account-password-${System.nanoTime()};MODE=MySQL;NON_KEYWORDS=USER",
    ).also { connection ->
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE user (
                    userid INT PRIMARY KEY,
                    password VARCHAR(64) NOT NULL,
                    salt VARCHAR(64) NOT NULL,
                    passworddate TIMESTAMP
                )
                """.trimIndent(),
            )
        }
    }
}
