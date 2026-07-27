package net.dodian.uber.game.engine.systems.net

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.loop.GameCycleClock
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.codec.ByteMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val SEND_SIDE_TAB_OPCODE = 106
private const val TEST_ITEM_ID = 995

class PacketMagicServiceCooldownTest {
    private var previousItemManager: ItemManager? = null

    @BeforeEach
    fun setUp() {
        previousItemManager = Server.itemManager
        Server.itemManager = ItemManager(definitionLoader = { emptyMap() }, globalSpawnBootstrap = {})
        GameCycleClock.syncTo(100)
    }

    @AfterEach
    fun tearDown() {
        Server.itemManager = previousItemManager
        GameCycleClock.syncTo(0)
    }

    @Test
    fun `casting again within 3 cycles of the last cast is blocked`() {
        val client = client()
        client.lastMagicCycle = GameCycleClock.currentCycle()

        // castSpell 0 matches no enchant/alchemy branch, so a non-null response can only mean
        // the cooldown gate itself fired (see PacketMagicService.handleMagicOnItem's OR check).
        PacketMagicService.handleMagicOnItem(client, 0, TEST_ITEM_ID, 0)

        val message = (client.channel as EmbeddedChannel).readOutbound<ByteMessage>()
        assertEquals(SEND_SIDE_TAB_OPCODE, message.opcode)
    }

    @Test
    fun `casting is allowed again once 3 cycles have passed`() {
        val client = client()
        client.lastMagicCycle = GameCycleClock.currentCycle()
        GameCycleClock.advance()
        GameCycleClock.advance()
        GameCycleClock.advance()

        PacketMagicService.handleMagicOnItem(client, 0, TEST_ITEM_ID, 0)

        assertNull(
            (client.channel as EmbeddedChannel).readOutbound<ByteMessage>(),
            "cooldown should have expired, and no recognized spell means nothing else should be sent either",
        )
    }

    private fun client(): Client =
        Client(EmbeddedChannel(), 1).apply {
            playerItems[0] = TEST_ITEM_ID + 1
            playerItemsN[0] = 1
        }
}
