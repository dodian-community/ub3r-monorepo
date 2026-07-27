package net.dodian.uber.game.model.item

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.uber.game.engine.systems.world.item.Ground
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.codec.ByteMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GroundItemRegionEntryPlaneTest {
    private var addedItem: GroundItem? = null

    @AfterEach
    fun tearDown() {
        addedItem?.let { Ground.ground_items.remove(it) }
        addedItem = null
    }

    @Test
    fun `region-entry replay does not send items on a different plane at the same x,y`() {
        // Same x/y as the viewer below, but one plane down (e.g. a dungeon under a surface tile).
        val item = GroundItem(Position(3200, 3200, 1), 995, 1, 0, true)
        addedItem = item
        Ground.ground_items += item

        val viewer = Client(EmbeddedChannel(), 1).apply {
            moveTo(3200, 3200, 0)
        }

        viewer.updateGroundItems()

        assertNull(
            (viewer.channel as EmbeddedChannel).readOutbound<ByteMessage>(),
            "an item on a different plane at the same x,y must not be replayed to the viewer",
        )
    }
}
