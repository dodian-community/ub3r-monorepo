package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.engine.systems.net.PacketInteractionRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of DuelRequest (opcode 153).
 * Decodes pid, then delegates all logic to PacketInteractionRequestService.
 */
public class DuelRequestListener implements PacketListener {

    static { PacketListenerManager.register(153, new DuelRequestListener()); }

    private static final Logger logger = LoggerFactory.getLogger(DuelRequestListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        int size = packet.size();
        if (size <= 0 || size > 8 || buf.readableBytes() < size) {
            return;
        }
        int temp = 0;
        int multiplier = 1000;
        for (int idx = 0; idx < size; idx++) {
            temp += buf.readUnsignedByte() * multiplier;
            if (multiplier > 1) {
                multiplier /= 1000;
            }
        }
        int pid = temp / 1000;

        Client other = client.getClient(pid);
        if (!client.validClient(pid) || client.getSlot() == pid) {
            return;
        }
        logger.debug("{} sent duel request to {} (slot {})", client.getPlayerName(), other.getPlayerName(), pid);
        PacketInteractionRequestService.handleDuelRequest(client, pid, other);
    }
}
