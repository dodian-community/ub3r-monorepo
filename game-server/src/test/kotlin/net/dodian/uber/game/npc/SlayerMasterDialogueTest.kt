package net.dodian.uber.game.npc

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.SlayerTaskState
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlayerMasterDialogueTest {
    @Test
    fun `already has task dialogue continues and closes cleanly`() {
        val client = client(91)
        client.slayerTaskState =
            SlayerTaskState(
                402,
                0,
                25,
                25,
                3,
                0,
                -1,
            )

        SlayerMasterDialogue.startIntro(client, npcId = 402)
        assertTrue(DialogueService.hasActiveSession(client))

        assertTrue(DialogueService.onContinue(client))
        assertTrue(DialogueService.onOption(client, 1))
        assertTrue(DialogueService.onOption(client, 1))
        assertTrue(DialogueService.hasActiveSession(client))

        assertTrue(DialogueService.onContinue(client))
        assertFalse(DialogueService.hasActiveSession(client))
        assertFalse(DialogueService.onContinue(client))
    }

    private fun client(slot: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.playerName = "slayer-dialogue-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.validClient = true
        client.teleportTo(3096, 3490, 0)
        return client
    }
}
