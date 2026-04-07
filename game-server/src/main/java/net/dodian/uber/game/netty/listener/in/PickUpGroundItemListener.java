package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.interaction.PlayerTickThrottleService;
import net.dodian.uber.game.systems.net.PacketPickupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of PickUpGroundItem (opcode 236).
 * Decodes coordinates, applies throttle, then delegates to PacketPickupService.
 */
public class PickUpGroundItemListener implements PacketListener {

    static { PacketListenerManager.register(236, new PickUpGroundItemListener()); }

    private static final Logger logger = LoggerFactory.getLogger(PickUpGroundItemListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        int itemY  = buf.readUnsignedShortLE();
        int itemId = buf.readUnsignedShort();
        int itemX  = buf.readUnsignedShortLE();

        if (!PlayerTickThrottleService.tryAcquireMs(client, PlayerTickThrottleService.PICKUP_GROUND_ITEM, 600L) ||
                (client.attemptGround != null
                        && client.attemptGround.id == itemId
                        && client.attemptGround.x == itemX
                        && client.attemptGround.y == itemY
                        && client.attemptGround.z == client.getPosition().getZ())) {
            return;
        }

        PacketPickupService.handle(client, itemId, itemX, itemY);

        logger.debug("PickUpGroundItemListener: {} attempts to pick item {} at ({},{})", client.getPlayerName(), itemId, itemX, itemY);
    }
}
