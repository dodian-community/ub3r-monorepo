package net.dodian.uber.game.engine.systems.inventory

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.netty.codec.ByteMessage
import net.dodian.uber.game.model.entity.player.Client
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private const val UPDATE_ITEM_CONTAINER_OPCODE = 53
private const val UPDATE_ITEM_SLOT_OPCODE = 34

class ClientInventoryDeltaTest {
    @Test
    fun `first resetItems call sends a full container packet`() {
        val channel = EmbeddedChannel()
        val client = Client(channel, 1).apply {
            playerItems[0] = 101
            playerItemsN[0] = 1
        }

        client.resetItems(3214)

        val message = channel.readOutbound<ByteMessage>()
        assertEquals(UPDATE_ITEM_CONTAINER_OPCODE, message.opcode)
        assertNull(channel.readOutbound<ByteMessage>(), "only one packet should be sent")
    }

    @Test
    fun `changing one slot sends only that slot after the initial full send`() {
        val channel = EmbeddedChannel()
        val client = Client(channel, 1).apply {
            playerItems[0] = 101
            playerItemsN[0] = 1
        }
        client.resetItems(3214)
        channel.readOutbound<ByteMessage>() // drain the initial full send

        client.playerItems[1] = 201
        client.playerItemsN[1] = 1
        client.resetItems(3214)

        val message = channel.readOutbound<ByteMessage>()
        assertEquals(UPDATE_ITEM_SLOT_OPCODE, message.opcode)
        assertNull(channel.readOutbound<ByteMessage>(), "only the changed slot should be sent")
    }

    @Test
    fun `no packet is sent when nothing changed`() {
        val channel = EmbeddedChannel()
        val client = Client(channel, 1).apply {
            playerItems[0] = 101
            playerItemsN[0] = 1
        }
        client.resetItems(3214)
        channel.readOutbound<ByteMessage>() // drain the initial full send

        client.resetItems(3214)

        assertNull(channel.readOutbound<ByteMessage>(), "nothing changed, nothing should be sent")
    }

    @Test
    fun `changing more slots than the threshold falls back to a full resend`() {
        val channel = EmbeddedChannel()
        val client = Client(channel, 1).apply {
            playerItems[0] = 101
            playerItemsN[0] = 1
        }
        client.resetItems(3214)
        channel.readOutbound<ByteMessage>() // drain the initial full send

        for (slot in 1..11) {
            client.playerItems[slot] = 200 + slot
            client.playerItemsN[slot] = 1
        }
        client.resetItems(3214)

        val message = channel.readOutbound<ByteMessage>()
        assertEquals(UPDATE_ITEM_CONTAINER_OPCODE, message.opcode)
        assertNull(channel.readOutbound<ByteMessage>(), "only one packet should be sent")
    }
}
