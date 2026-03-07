package net.dodian.uber.game.model.entity

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EntityFaceCoordinateTest {
    @Test
    fun `setFocus supports underground coordinate targets`() {
        val client = Client(EmbeddedChannel(), 1)
        try {
            assertDoesNotThrow {
                client.setFocus(2724, 9753)
            }

            assertEquals((2724 * 2) + 1, client.faceCoordinateX)
            assertEquals((9753 * 2) + 1, client.faceCoordinateY)
        } finally {
            (client.channel as EmbeddedChannel).finishAndReleaseAll()
        }
    }

    @Test
    fun `setFocus clamps packet coordinates to unsigned short range`() {
        val client = Client(EmbeddedChannel(), 1)
        try {
            client.setFocus(40000, 40000)

            assertEquals(0xFFFF, client.faceCoordinateX)
            assertEquals(0xFFFF, client.faceCoordinateY)
        } finally {
            (client.channel as EmbeddedChannel).finishAndReleaseAll()
        }
    }
}
