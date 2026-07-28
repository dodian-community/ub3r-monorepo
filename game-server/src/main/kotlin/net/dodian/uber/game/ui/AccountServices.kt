package net.dodian.uber.game.ui

import net.dodian.uber.game.engine.loop.GameThreadIngress
import net.dodian.uber.game.engine.tasking.PlayerScopedCoroutineService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendPasswordPrompt
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.persistence.account.AccountPasswordRepository
import net.dodian.uber.game.persistence.account.AccountPersistenceService
import net.dodian.uber.game.persistence.repository.DbAsyncRepository
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object AccountServices {
    private val logger = LoggerFactory.getLogger(AccountServices::class.java)
    private const val ACCOUNT_INTERFACE_ID = 36700
    private const val CURRENT_STAGE = 1
    private const val NEW_STAGE = 2
    private const val AUTHORIZATION_TTL_MS = 2 * 60 * 1000L
    private const val FAILURE_BLOCK_MS = 5 * 60 * 1000L
    private const val MAX_FAILURES = 5
    private const val MAX_PASSWORD_LENGTH = 20
    private const val MAX_RSA_BYTES = 512
    private const val MAX_FAILURE_SUBJECTS = 10_000
    private val sessions = ConcurrentHashMap<Client, PasswordChangeSession>()
    private val failures = ConcurrentHashMap<Int, PasswordFailureState>()

    @JvmStatic
    fun open(client: Client) {
        cancelPasswordChange(client)
        client.viewingAccountServices = true

        // Account Status first, Change Password second
        client.send(SendString("Account Status", 36732))
        client.send(SendString("Change Password", 36734))

        // Clear remaining slots (36736 to 36770)
        for (stringId in 36736..36770 step 2) {
            client.send(SendString("", stringId))
        }

        // Title
        client.send(SendString("Account Services", 36705))

        // Show player details initially
        showDefaultStatus(client)

        client.openInterface(ACCOUNT_INTERFACE_ID)
    }

    @JvmStatic
    fun handleRowClick(client: Client, index: Int) {
        if (index == 0) { // Account Status
            cancelPasswordChange(client)
            showDefaultStatus(client)
        } else if (index == 1) { // Change Password
            val now = System.currentTimeMillis()
            if (isPasswordChangeBlocked(client.dbId, now)) {
                client.send(SendString("<col=ff0000>Too many incorrect attempts. Try again in 5 minutes.</col>", 36712))
                return
            }
            client.send(SendString("Change Password", 36706))
            client.send(SendString("Step 1:", 36707))
            client.send(SendString("Type current password in prompt", 36708))
            client.send(SendString("Step 2:", 36709))
            client.send(SendString("Type new password in prompt", 36710))
            client.send(SendString("Status:", 36711))
            client.send(SendString("Awaiting current password...", 36712))
            client.send(SendString("", 36713))
            client.send(SendString("", 36714))
            beginStage(client, CURRENT_STAGE, null)
        }
    }

    @JvmStatic
    fun handleEncryptedPasswordInput(client: Client, encryptedPayload: ByteArray) {
        val expected = sessions.remove(client) ?: return
        if (!sessionIsCurrent(client, expected)) {
            cancelPasswordChange(client)
            return
        }
        PlayerScopedCoroutineService.launch(client, "account-password-input", AccountPersistenceService.scope) {
            val decoded = decryptPasswordInput(encryptedPayload)
            val password = decoded?.password?.concatToString().orEmpty()
            if (decoded == null ||
                decoded.stage != expected.stage ||
                !constantTimeEquals(decoded.nonce, expected.nonce) ||
                !validPassword(password)
            ) {
                decoded?.clear()
                passwordUiError(client, "Invalid or expired password request.")
                return@launch
            }
            try {
                if (decoded.stage == CURRENT_STAGE) {
                    verifyCurrentPassword(client, expected, password)
                } else {
                    updatePassword(client, expected, password)
                }
            } finally {
                decoded.clear()
                encryptedPayload.fill(0)
            }
        }
    }

    @JvmStatic
    fun cancelPasswordChange(client: Client) {
        sessions.remove(client)
    }

    @JvmStatic
    fun close(client: Client) {
        cancelPasswordChange(client)
        client.viewingAccountServices = false
    }

    private fun beginStage(client: Client, stage: Int, verifiedHash: String?) {
        val nonce = UUID.randomUUID().toString().replace("-", "")
        sessions[client] = PasswordChangeSession(
            dbId = client.dbId,
            channelId = client.channel?.id()?.asLongText().orEmpty(),
            stage = stage,
            nonce = nonce,
            expiresAt = System.currentTimeMillis() + AUTHORIZATION_TTL_MS,
            verifiedHash = verifiedHash,
        )
        val title = if (stage == CURRENT_STAGE) "Enter current password:" else "Enter new password:"
        client.send(SendPasswordPrompt(stage, nonce, title))
    }

    private suspend fun verifyCurrentPassword(
        client: Client,
        expected: PasswordChangeSession,
        inputPassword: String,
    ) {
        if (isPasswordChangeBlocked(expected.dbId, System.currentTimeMillis())) {
            passwordUiError(client, "Too many incorrect attempts. Try again in 5 minutes.")
            return
        }
        val verifiedRow = DbAsyncRepository.withConnection { connection ->
            AccountPasswordRepository.verifyCurrentPassword(connection, expected.dbId, inputPassword)
        }
        if (verifiedRow == null) {
            val blocked = recordPasswordFailure(expected.dbId)
            logger.warn("Rejected in-game password verification dbId={} blocked={}", expected.dbId, blocked)
            passwordUiError(
                client,
                if (blocked) "Too many incorrect attempts. Try again in 5 minutes."
                else "Incorrect current password. Password change aborted.",
            )
            return
        }
        failures.remove(expected.dbId)
        GameThreadIngress.submitCritical("account-password-verified") {
            if (!client.disconnected && client.viewingAccountServices && client.dbId == expected.dbId) {
                client.send(SendString("<col=00ff00>Current password verified.</col>", 36712))
                client.send(SendString("Awaiting new password...", 36714))
                beginStage(client, NEW_STAGE, verifiedRow.storedHash)
            }
        }
    }

    private suspend fun updatePassword(
        client: Client,
        expected: PasswordChangeSession,
        newPassword: String,
    ) {
        val verifiedHash = expected.verifiedHash
        if (verifiedHash.isNullOrEmpty()) {
            passwordUiError(client, "Password authorization expired.")
            return
        }
        val result: PasswordUpdateResult = try {
            when (DbAsyncRepository.withConnection { connection ->
                AccountPasswordRepository.updatePassword(connection, expected.dbId, verifiedHash, newPassword)
            }) {
                AccountPasswordRepository.UpdateResult.UPDATED -> PasswordUpdateResult.UPDATED
                AccountPasswordRepository.UpdateResult.STALE -> PasswordUpdateResult.STALE
            }
        } catch (exception: Exception) {
            logger.warn("Database error changing password for dbId={}", expected.dbId, exception)
            PasswordUpdateResult.ERROR
        }
        GameThreadIngress.submitCritical("account-password-updated") {
            if (client.disconnected || client.dbId != expected.dbId) return@submitCritical
            when (result) {
                PasswordUpdateResult.UPDATED -> {
                    client.send(SendString("<col=00ff00>Password updated successfully!</col>", 36712))
                    client.send(SendString("", 36714))
                    client.sendMessage("Your password has been successfully updated!")
                }
                PasswordUpdateResult.STALE ->
                    client.send(SendString("<col=ff0000>Password changed elsewhere. Start again.</col>", 36712))
                PasswordUpdateResult.ERROR ->
                    client.send(SendString("<col=ff0000>Unable to update password.</col>", 36712))
            }
        }
    }

    private fun sessionIsCurrent(client: Client, session: PasswordChangeSession): Boolean =
        !client.disconnected &&
            client.viewingAccountServices &&
            client.activeInterfaceId == ACCOUNT_INTERFACE_ID &&
            client.dbId > 0 &&
            client.dbId == session.dbId &&
            client.channel?.id()?.asLongText().orEmpty() == session.channelId &&
            System.currentTimeMillis() <= session.expiresAt

    private fun decryptPasswordInput(payload: ByteArray): DecryptedPasswordInput? {
        if (payload.size !in 1..MAX_RSA_BYTES) return null
        return try {
            var decoded = BigInteger(1, payload)
                .modPow(net.dodian.uber.game.engine.config.rsaExponent, net.dodian.uber.game.engine.config.rsaModulus)
                .toByteArray()
            if (decoded.isNotEmpty() && decoded[0].toInt() == 0) decoded = decoded.copyOfRange(1, decoded.size)
            val cursor = PasswordCursor(decoded)
            if (cursor.unsignedByte() != 10) return null
            val stage = cursor.unsignedByte()
            val nonce = cursor.string(64)
            val password = cursor.string(MAX_PASSWORD_LENGTH)
            if (cursor.remaining() != 0) null else DecryptedPasswordInput(stage, nonce, password.toCharArray())
        } catch (_: Exception) {
            null
        }
    }

    private fun validPassword(password: String): Boolean =
        password.isNotEmpty() &&
            password.length <= MAX_PASSWORD_LENGTH &&
            password.all { it.code in 32..126 || it == '£' }

    private fun passwordUiError(client: Client, message: String) {
        GameThreadIngress.submitCritical("account-password-error") {
            if (!client.disconnected) {
                client.send(SendString("<col=ff0000>$message</col>", 36712))
                client.sendMessage(message)
            }
        }
    }

    private fun recordPasswordFailure(dbId: Int): Boolean {
        val now = System.currentTimeMillis()
        if (failures.size >= MAX_FAILURE_SUBJECTS && !failures.containsKey(dbId)) {
            failures.entries.removeIf { (_, state) ->
                synchronized(state) {
                    now >= state.blockedUntil && now - state.lastSeenAt >= FAILURE_BLOCK_MS
                }
            }
            if (failures.size >= MAX_FAILURE_SUBJECTS) return true
        }
        val state = failures.computeIfAbsent(dbId) { PasswordFailureState() }
        synchronized(state) {
            state.lastSeenAt = now
            if (now < state.blockedUntil) return true
            state.failures++
            if (state.failures < MAX_FAILURES) return false
            state.failures = 0
            state.blockedUntil = now + FAILURE_BLOCK_MS
            return true
        }
    }

    private fun isPasswordChangeBlocked(dbId: Int, now: Long): Boolean {
        if (dbId <= 0) return true
        val state = failures[dbId] ?: return false
        synchronized(state) {
            if (now < state.blockedUntil) return true
            if (state.blockedUntil != 0L) failures.remove(dbId, state)
            return false
        }
    }

    private fun constantTimeEquals(left: String, right: String): Boolean =
        MessageDigest.isEqual(
            left.lowercase().toByteArray(StandardCharsets.US_ASCII),
            right.lowercase().toByteArray(StandardCharsets.US_ASCII),
        )

    private data class PasswordChangeSession(
        val dbId: Int,
        val channelId: String,
        val stage: Int,
        val nonce: String,
        val expiresAt: Long,
        val verifiedHash: String?,
    )

    private data class DecryptedPasswordInput(
        val stage: Int,
        val nonce: String,
        val password: CharArray,
    ) {
        fun clear() = password.fill('\u0000')
    }

    private class PasswordFailureState {
        var failures: Int = 0
        var blockedUntil: Long = 0L
        var lastSeenAt: Long = System.currentTimeMillis()
    }

    private enum class PasswordUpdateResult { UPDATED, STALE, ERROR }

    private class PasswordCursor(private val bytes: ByteArray) {
        private var index = 0

        fun remaining(): Int = bytes.size - index

        fun unsignedByte(): Int {
            require(remaining() >= 1)
            return bytes[index++].toInt() and 0xff
        }

        fun string(maxLength: Int): String {
            val start = index
            while (index < bytes.size && bytes[index].toInt() != 10 && bytes[index].toInt() != 0) {
                require(index - start < maxLength)
                index++
            }
            require(index < bytes.size)
            val value = String(bytes, start, index - start, StandardCharsets.ISO_8859_1)
            index++
            return value
        }
    }

    @JvmStatic
    fun handleAccountServicesDialogue(client: Client, option: Int) {
        if (client.accountServicesDialogState == 3) {
            client.accountServicesDialogState = 0
            client.send(net.dodian.uber.game.netty.listener.out.RemoveInterfaces())
        }
    }

    @JvmStatic
    fun showDefaultStatus(client: Client) {
        val now = System.currentTimeMillis()
        val muteStatus = if (client.mutedTill <= now) {
            "Not Muted"
        } else {
            val remainingMs = client.mutedTill - now
            val hours = remainingMs / 3600000L
            val minutes = (remainingMs % 3600000L) / 60000L
            "Muted (${hours}h ${minutes}m)"
        }

        val token = ++client.accountServiceRequestToken
        PlayerScopedCoroutineService.launch(client, "account-status", AccountPersistenceService.scope) {
            try {
                val status = DbAsyncRepository.withConnection { conn ->
                    try {
                        val query = "SELECT c.name, u.joindate, u.usergroupid " +
                                       "FROM characters c " +
                                       "LEFT JOIN user u ON LOWER(c.name) = LOWER(u.username) " +
                                       "WHERE LOWER(c.name) = ?"
                        val ps = conn.prepareStatement(query)
                        ps.setString(1, client.playerName.lowercase())
                        val rs = ps.executeQuery()
                        var createdStr = "N/A"
                        var rankStr = "Player"
                        if (rs.next()) {
                            val joinSeconds = rs.getLong("joindate")
                            if (joinSeconds > 0) {
                                val sdf = SimpleDateFormat("yyyy-MM-dd")
                                createdStr = sdf.format(Date(joinSeconds * 1000L))
                            }
                            val mgroup = rs.getInt("usergroupid")
                            val rights = if (mgroup == 9 || mgroup == 5) 1 else if (mgroup == 6 || mgroup == 18 || mgroup == 10) 2 else 0
                            rankStr = if (rights == 1) "Moderator" else if (rights >= 2) "Administrator" else "Player"
                        }
                        Triple(client.playerName, rankStr, createdStr)
                    } catch (e: Exception) {
                        logger.warn("Database error loading account status for {}", client.playerName, e)
                        null
                    }
                }
                GameThreadIngress.submitCritical("account-status") {
                    if (client.accountServiceRequestToken != token) return@submitCritical
                    if (status != null) {
                        val (name, rank, created) = status
                        client.send(SendString(name, 36706))
                        client.send(SendString("Rank:", 36707))
                        client.send(SendString(rank, 36708))
                        client.send(SendString("Created:", 36709))
                        client.send(SendString(created, 36710))
                        client.send(SendString("Mute Status:", 36711))
                        client.send(SendString(muteStatus, 36712))
                        client.send(SendString("IP Address:", 36713))
                        client.send(SendString(client.connectedFrom, 36714))
                    } else {
                        fallbackStatusDisplay(client, muteStatus)
                    }
                }
            } catch (e: Exception) {
                GameThreadIngress.submitCritical("account-status-error") {
                    if (client.accountServiceRequestToken != token) return@submitCritical
                    fallbackStatusDisplay(client, muteStatus)
                }
            }
        }
    }

    private fun fallbackStatusDisplay(client: Client, muteStatus: String) {
        client.send(SendString(client.playerName, 36706))
        client.send(SendString("Rank:", 36707))
        client.send(SendString("Player", 36708))
        client.send(SendString("Created:", 36709))
        client.send(SendString("N/A", 36710))
        client.send(SendString("Mute Status:", 36711))
        client.send(SendString(muteStatus, 36712))
        client.send(SendString("IP Address:", 36713))
        client.send(SendString(client.connectedFrom, 36714))
    }
}
