package net.dodian.uber.game.netty.login

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginProcessorHandlerTest {
    @Test
    fun `reports dedicated authentication block reasons`() {
        assertEquals("Incorrect password", LoginProcessorHandler.describeReason(3))
        assertEquals("Authentication blocked for five minutes", LoginProcessorHandler.describeReason(34))
        assertEquals("Authentication blocked for 24 hours", LoginProcessorHandler.describeReason(35))
    }

    @Test
    fun `parsed login mode distinguishes login from account creation`() {
        val login = ParsedLoginRequest(
            ParsedLoginRequest.Mode.LOGIN,
            "Neosback",
            "password",
            "",
            null,
            null,
            0,
        )
        val create = ParsedLoginRequest(
            ParsedLoginRequest.Mode.CREATE_ACCOUNT,
            "Newplayer",
            "password",
            "oauth-code",
            null,
            null,
            0,
        )

        assertEquals(false, login.isAccountCreationRequested)
        assertEquals(true, create.isAccountCreationRequested)
    }

    @Test
    fun `reports a clear reason for a duplicate Discord account username`() {
        assertEquals(
            "Username already exists; choose another username",
            LoginProcessorHandler.describeReason(33),
        )
    }
}
