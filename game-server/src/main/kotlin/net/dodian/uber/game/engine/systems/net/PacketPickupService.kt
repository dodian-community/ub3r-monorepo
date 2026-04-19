package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.engine.lifecycle.PlayerDeferredLifecycleService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.engine.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.engine.state.GroundItemIntentStateAdapter
import net.dodian.uber.game.engine.systems.world.item.Ground
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Kotlin service for ground-item pickup packet logic (opcode 236).
 *
 * The full business logic — guard messages, action cancellation, ground-item
 * lookup, pickup scheduling — lives here rather than in the listener.
 */
object PacketPickupService {
    private val RING_REPEAT_BLOCK_CUTOFF_EPOCH_MS: Long =
        LocalDate.of(2024, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /**
     * Processes a pick-up-ground-item request after the listener has decoded
     * item id and world coordinates and passed throttle checks.
     *
     * @param client the requesting player
     * @param itemId decoded item id
     * @param itemX  decoded world X coordinate
     * @param itemY  decoded world Y coordinate
     */
    @JvmStatic
    fun handle(client: Client, itemId: Int, itemX: Int, itemY: Int) {
        if (client.randomed || client.UsingAgility) return

        if (itemId in 5509..5515 && client.checkItem(itemId)) {
            client.send(SendMessage("You already got this item!"))
            return
        }

        if (itemId == 7927 && System.currentTimeMillis() < RING_REPEAT_BLOCK_CUTOFF_EPOCH_MS && client.checkItem(7927)) {
            client.send(SendMessage("You already got this ring! Wait until after May!"))
            return
        }

        PlayerActionCancellationService.cancel(
            client,
            PlayerActionCancelReason.GROUND_ITEM_INTERACTION,
            false, false, false, true
        )
        val target = Ground.findGroundItem(client, itemId, itemX, itemY, client.position.getZ())
        if (target == null) {
            GroundItemIntentStateAdapter.clearPickup(client)
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(client)
            return
        }
        if (client.position.getX() != itemX || client.position.getY() != itemY) {
            GroundItemIntentStateAdapter.beginPickup(client, target)
            PlayerDeferredLifecycleService.scheduleGroundPickupArrivalWatch(client, target)
        } else {
            GroundItemIntentStateAdapter.beginPickup(client, target)
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(client)
            client.pickUpItem(itemX, itemY)
        }
    }
}
