package net.dodian.uber.game.persistence.account

import net.dodian.uber.game.persistence.account.login.AccountLoginService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AccountPersistenceServiceTest {
    @Test
    fun `existing creation username is rejected during preflight`() {
        var usernameChecks = 0

        val code = AccountPersistenceService.accountCreationPreflightCode(
            accountCreationRequested = true,
            discordAuthCode = "authorization",
            usernameExists = {
                usernameChecks++
                true
            },
        )

        assertEquals(AccountLoginService.USERNAME_TAKEN, code)
        assertEquals(1, usernameChecks)
    }

    @Test
    fun `ordinary login bypasses creation preflight`() {
        val code = AccountPersistenceService.accountCreationPreflightCode(
            accountCreationRequested = false,
            discordAuthCode = "",
            usernameExists = { error("ordinary login must not query creation collisions") },
        )

        assertNull(code)
    }
}
