package net.dodian.uber.game.skill.runtime.action

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkillStateCoordinatorTest {
    @Test
    fun `begin session prevents dual active sessions for different keys`() {
        val client = testClient(slot = 6101)

        assertTrue(SkillStateCoordinator.beginSession(client, "skill.mining"))
        assertEquals("skill.mining", client.activeSkillSessionKey)
        assertFalse(SkillStateCoordinator.beginSession(client, "skill.fishing"))
        assertEquals("skill.mining", client.activeSkillSessionKey)
    }

    @Test
    fun `interrupt session clears active key`() {
        val client = testClient(slot = 6102)

        assertTrue(SkillStateCoordinator.beginSession(client, "skill.woodcutting"))
        assertEquals("skill.woodcutting", client.activeSkillSessionKey)
        SkillStateCoordinator.interruptSession(client)
        assertNull(client.activeSkillSessionKey)
    }

    private fun testClient(slot: Int): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.longName = slot.toLong()
        client.playerName = "skill-state-$slot"
        client.isActive = true
        client.initialized = true
        client.disconnected = false
        client.pLoaded = true
        client.dbId = slot
        return client
    }
}
