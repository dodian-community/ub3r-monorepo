package net.dodian.uber.game.systems.net

import net.dodian.uber.game.engine.lifecycle.PlayerDeferredLifecycleService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.action.PlayerActionCancelReason
import net.dodian.uber.game.systems.action.PlayerActionCancellationService
import net.dodian.uber.game.systems.world.item.Ground
import java.util.Date

/**
 * Kotlin service for ground-item pickup packet logic (opcode 236).
 *
 * The full business logic — guard messages, action cancellation, ground-item
 * lookup, pickup scheduling — lives here rather than in the listener.
 */
object PacketPickupService {

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

        try {
            @Suppress("DEPRECATION")
            if (itemId == 7927 && Date().before(Date("06/1/2024")) && client.checkItem(7927)) {
                client.send(SendMessage("You already got this ring! Wait until after May!"))
                return
            }
        } catch (_: Exception) {
            // date parse fallback; ignore
        }

        PlayerActionCancellationService.cancel(
            client,
            PlayerActionCancelReason.GROUND_ITEM_INTERACTION,
            false, false, false, true
        )
        client.attemptGround = Ground.findGroundItem(client, itemId, itemX, itemY, client.getPosition().getZ())
        if (client.attemptGround == null) {
            client.pickupWanted = false
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(client)
            return
        }
        if (client.getPosition().getX() != itemX || client.getPosition().getY() != itemY) {
            client.pickupWanted = true
            PlayerDeferredLifecycleService.scheduleGroundPickupArrivalWatch(client, client.attemptGround)
        } else {
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(client)
            client.pickUpItem(itemX, itemY)
        }
    }
}

