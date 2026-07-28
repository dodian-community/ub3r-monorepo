package net.dodian.uber.game.netty.login

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginProcessorHandlerTest {
    @Test
    fun `reports a clear reason for a duplicate Discord account username`() {
        assertEquals(
            "Username already exists; choose another username",
            LoginProcessorHandler.describeReason(33),
        )
    }
}
