package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.net.PacketInteractionRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of legacy Trade packet (opcode 139).
 * Decodes target slot, then delegates all logic to PacketInteractionRequestService.
 */
public class TradeListener implements PacketListener {

    static { PacketListenerManager.register(139, new TradeListener()); }

    private static final Logger logger = LoggerFactory.getLogger(TradeListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        int targetSlot = buf.readShortLE() & 0xFFFF;
        Client other = client.getClient(targetSlot);
        if (!client.validClient(targetSlot)) {
            return;
        }
        PacketInteractionRequestService.handleTradeRequest(client, targetSlot, other);
        if (logger.isTraceEnabled()) {
            logger.trace("{} sent Trade request to slot {} ({})", client.getPlayerName(), targetSlot, other.getPlayerName());
        }
    }
}
