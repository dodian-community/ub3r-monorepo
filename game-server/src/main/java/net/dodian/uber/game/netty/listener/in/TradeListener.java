package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opcode 139 now carries slot-4 player menu clicks, which are used for Follow.
 */
public class TradeListener implements PacketListener {

    static { PacketListenerManager.register(139, new TradeListener()); }

    private static final Logger logger = LoggerFactory.getLogger(TradeListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        int targetSlot = buf.readShortLE() & 0xFFFF;
        PlayerClickListener.handleFollowPlayer(client, targetSlot);
        if (logger.isTraceEnabled()) {
            logger.trace("{} sent Follow request to slot {}", client.getPlayerName(), targetSlot);
        }
    }
}
