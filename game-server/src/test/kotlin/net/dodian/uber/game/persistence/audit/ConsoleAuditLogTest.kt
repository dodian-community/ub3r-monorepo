package net.dodian.uber.game.persistence.audit

import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ConsoleAuditLogTest {
    @Test
    fun `player reference labels the database id explicitly`() {
        val player = testClient(slot = 1, name = "Admin", dbId = 1)

        assertEquals("player=Admin | dbId=1", ConsoleAuditLog.playerRef(player))
    }

    @Test
    fun `public chat audit text omits receiver field`() {
        val player = testClient(slot = 1, name = "Admin", dbId = 1)

        val text = ConsoleAuditLog.chatAuditText(channel = "PUBLIC", player = player, message = "Hi", receiver = null)

        assertEquals("PUBLIC CHAT | player=Admin | dbId=1 | msg=\"Hi\"", text)
        assertFalse(text.contains("receiver="))
    }

    @Test
    fun `private chat audit text keeps explicit receiver`() {
        val sender = testClient(slot = 1, name = "Admin", dbId = 1)
        val receiver = testClient(slot = 2, name = "Mod", dbId = 42)

        val text = ConsoleAuditLog.chatAuditText(channel = "PRIVATE", player = sender, message = "Hello", receiver = receiver)

        assertEquals(
            "PRIVATE CHAT | player=Admin | dbId=1 | receiver=player=Mod | dbId=42 | msg=\"Hello\"",
            text,
        )
    }

    private fun testClient(slot: Int, name: String, dbId: Int): Client =
        Client(null, slot).apply {
            playerName = name
            this.dbId = dbId
        }
}
