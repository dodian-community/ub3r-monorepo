package net.dodian.uber.game.persistence.audit

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.ui.buttons.ButtonClickRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun `button audit text shows raw button id first for handled clicks`() {
        val player = testClient(slot = 1, name = "Admin", dbId = 1)
        val request =
            ButtonClickRequest(
                client = player,
                rawButtonId = 9154,
                opIndex = 0,
                activeInterfaceId = 3824,
                interfaceId = 3824,
                componentId = 18,
                componentKey = "3824:18",
            )

        val text = ConsoleAuditLog.buttonAuditText(request, opcode = 185, handled = true)

        assertEquals(
            "BUTTON OK | buttonId=9154 | opcode=185 | activeInterface=3824 | interface=3824 | componentId=18 | opIndex=0 | player=Admin | dbId=1",
            text,
        )
    }

    @Test
    fun `button audit text includes key for unhandled clicks`() {
        val player = testClient(slot = 1, name = "Admin", dbId = 1)
        val request =
            ButtonClickRequest(
                client = player,
                rawButtonId = 12345,
                opIndex = -1,
                activeInterfaceId = 50000,
                interfaceId = -1,
                componentId = -1,
                componentKey = "raw:12345",
            )

        val text = ConsoleAuditLog.buttonAuditText(request, opcode = 185, handled = false)

        assertTrue(text.startsWith("BUTTON UNHANDLED | buttonId=12345 | opcode=185"))
        assertTrue(text.contains("| key=raw:12345 |"))
    }

    private fun testClient(slot: Int, name: String, dbId: Int): Client =
        Client(null, slot).apply {
            playerName = name
            this.dbId = dbId
        }
}
