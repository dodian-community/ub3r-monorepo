package net.dodian.uber.game.persistence.account

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Connection
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.persistence.db.DbTables

internal object AccountPasswordRepository {
    private const val SALT_LENGTH = 30
    private val secureRandom = SecureRandom()
    private const val saltAlphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    data class VerifiedPassword(val storedHash: String)
    data class NewCredential(val hash: String, val salt: String)
    enum class UpdateResult { UPDATED, STALE }

    fun createCredential(password: String): NewCredential {
        val salt = randomSalt()
        return NewCredential(Client.passHash(password, salt), salt)
    }

    fun verifyCurrentPassword(
        connection: Connection,
        dbId: Int,
        inputPassword: String,
    ): VerifiedPassword? =
        connection.prepareStatement(
            "SELECT password, salt FROM ${DbTables.WEB_USERS_TABLE} WHERE userid = ?",
        ).use { statement ->
            statement.setInt(1, dbId)
            statement.executeQuery().use { results ->
                if (!results.next()) return null
                val storedHash = results.getString("password").orEmpty()
                val salt = results.getString("salt").orEmpty()
                val candidate = Client.passHash(inputPassword, salt)
                if (constantTimeEquals(candidate, storedHash)) VerifiedPassword(storedHash) else null
            }
        }

    fun updatePassword(
        connection: Connection,
        dbId: Int,
        verifiedHash: String,
        newPassword: String,
    ): UpdateResult {
        val previousAutoCommit = connection.autoCommit
        connection.autoCommit = false
        try {
            val currentHash = connection.prepareStatement(
                "SELECT password FROM ${DbTables.WEB_USERS_TABLE} WHERE userid = ? FOR UPDATE",
            ).use { statement ->
                statement.setInt(1, dbId)
                statement.executeQuery().use { results ->
                    if (results.next()) results.getString("password").orEmpty() else null
                }
            }
            if (currentHash == null || !constantTimeEquals(currentHash, verifiedHash)) {
                connection.rollback()
                return UpdateResult.STALE
            }

            val credential = createCredential(newPassword)
            val updated = connection.prepareStatement(
                "UPDATE ${DbTables.WEB_USERS_TABLE} " +
                    "SET password = ?, salt = ?, passworddate = CURRENT_TIMESTAMP " +
                    "WHERE userid = ? AND password = ?",
            ).use { statement ->
                statement.setString(1, credential.hash)
                statement.setString(2, credential.salt)
                statement.setInt(3, dbId)
                statement.setString(4, currentHash)
                statement.executeUpdate()
            }
            return if (updated == 1) {
                connection.commit()
                UpdateResult.UPDATED
            } else {
                connection.rollback()
                UpdateResult.STALE
            }
        } catch (exception: Exception) {
            connection.rollback()
            throw exception
        } finally {
            connection.autoCommit = previousAutoCommit
        }
    }

    private fun randomSalt(): String = buildString(SALT_LENGTH) {
        repeat(SALT_LENGTH) { append(saltAlphabet[secureRandom.nextInt(saltAlphabet.length)]) }
    }

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(
            left.lowercase().toByteArray(StandardCharsets.US_ASCII),
            right.lowercase().toByteArray(StandardCharsets.US_ASCII),
        )
}
