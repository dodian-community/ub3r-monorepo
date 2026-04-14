package net.dodian.uber.game.npc

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegendsGuardDialogueTest {
    @Test
    fun `guard dialogue continue is consumed and closes session`() {
        val client = client(1)
        client.premium = true

        LegendsGuard.openDialogue(client, npcId = 3951)

        assertTrue(DialogueService.hasActiveSession(client))
        assertTrue(DialogueService.onContinue(client))
        assertFalse(DialogueService.hasActiveSession(client))
        assertFalse(DialogueService.onIndexedContinue(client))
        DialogueService.clear(client)
    }

    private fun client(slot: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.playerName = "legend-guard-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.teleportTo(2728, 3348, 0)
        return client
    }
}
