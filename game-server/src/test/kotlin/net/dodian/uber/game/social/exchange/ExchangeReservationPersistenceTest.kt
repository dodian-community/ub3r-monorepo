package net.dodian.uber.game.social.exchange

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.api.plugin.social.ExchangeKind
import net.dodian.uber.game.persistence.player.InventorySegmentSnapshot
import net.dodian.uber.game.persistence.player.PlayerSaveEnvelope
import net.dodian.uber.game.persistence.player.PlayerSaveReason
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExchangePersistenceGuardTest {
    @Test
    fun `reservation leaves the real inventory safe to persist`() {
        val player = Client(null, 1).apply {
            dbId = 10; playerName = "player"; playerItems[0] = 996; playerItemsN[0] = 5
        }
        val partner = Client(null, 2).apply { dbId = 20; playerName = "partner" }
        ExchangeRuntime.create(ExchangeKind.TRADE, player, partner)
        assertTrue(ExchangeRuntime.reserve(player, 0, 995, 3))

        val envelope = PlayerSaveEnvelope.fromClient(
            player, 1, PlayerSaveReason.PERIODIC, false, false, PlayerSaveSegment.ALL_MASK,
        )
        val inventory = envelope.segments.filterIsInstance<InventorySegmentSnapshot>().single()
        assertEquals(5, inventory.entries.single().amount)
        assertEquals(2, ExchangeRuntime.availableAt(player, 0, 995))
        ExchangeRuntime.remove(player)
    }
}
