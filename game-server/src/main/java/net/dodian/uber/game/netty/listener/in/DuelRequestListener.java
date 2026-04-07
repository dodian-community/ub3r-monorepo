package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.net.PacketInteractionRequestService;
import net.dodian.utilities.Utils;
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
        byte[] data = new byte[size];
        buf.readBytes(data);
        int pid = Utils.HexToInt(data, 0, size) / 1000;

        Client other = client.getClient(pid);
        if (!client.validClient(pid) || client.getSlot() == pid) {
            return;
        }
        logger.debug("{} sent duel request to {} (slot {})", client.getPlayerName(), other.getPlayerName(), pid);
        PacketInteractionRequestService.handleDuelRequest(client, pid, other);
    }
}
