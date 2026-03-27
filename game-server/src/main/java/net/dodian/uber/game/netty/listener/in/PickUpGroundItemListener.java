package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.runtime.action.PlayerActionCancellationService;
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason;
import net.dodian.uber.game.runtime.interaction.PlayerTickThrottleService;
import net.dodian.uber.game.runtime.lifecycle.PlayerDeferredLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Netty port of PickUpGroundItem (opcode 236).
 */
public class PickUpGroundItemListener implements PacketListener {

    static { PacketListenerManager.register(236, new PickUpGroundItemListener()); }

    private static final Logger logger = LoggerFactory.getLogger(PickUpGroundItemListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        int itemY  = buf.readUnsignedShortLE();   // little-endian as client sends
        int itemId = buf.readUnsignedShort();     // still big-endian for id
        int itemX  = buf.readUnsignedShortLE();   // little-endian

        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (itemId >= 5509 && itemId <= 5515 && client.checkItem(itemId)) {
            client.send(new SendMessage("You already got this item!"));
            return;
        }
        try {
            if (itemId == 7927 && new Date().before(new Date("06/1/2024")) && client.checkItem(7927)) {
                client.send(new SendMessage("You already got this ring! Wait until after May!"));
                return;
            }
        } catch (Exception e) {
            // date parse fallback; ignore
        }
        if (!PlayerTickThrottleService.tryAcquireMs(client, PlayerTickThrottleService.PICKUP_GROUND_ITEM, 600L) ||
                (client.attemptGround != null
                        && client.attemptGround.id == itemId
                        && client.attemptGround.x == itemX
                        && client.attemptGround.y == itemY
                        && client.attemptGround.z == client.getPosition().getZ())) {
            return;
        }
        PlayerActionCancellationService.cancel(client, PlayerActionCancelReason.GROUND_ITEM_INTERACTION, false, false, false, true);
        client.attemptGround = Ground.findGroundItem(client, itemId, itemX, itemY, client.getPosition().getZ());
        if (client.attemptGround == null) {
            client.pickupWanted = false;
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(client);
            return;
        }
        if (client.getPosition().getX() != itemX || client.getPosition().getY() != itemY) {
            client.pickupWanted = true;
            PlayerDeferredLifecycleService.scheduleGroundPickupArrivalWatch(client, client.attemptGround);
        } else {
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(client);
            client.pickUpItem(itemX, itemY);
        }

        logger.debug("PickUpGroundItemListener: {} attempts to pick item {} at ({},{})", client.getPlayerName(), itemId, itemX, itemY);
    }
}
