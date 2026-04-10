package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.net.PacketInteractionRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opcode 39 – slot-5 player menu click (Trade with in current menu mapping).
 */
public class FollowPlayerListener implements PacketListener {

    static { PacketListenerManager.register(39, new FollowPlayerListener()); }

    private static final Logger logger = LoggerFactory.getLogger(FollowPlayerListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < 2) {
            return;
        }
        int followId = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);

        if (logger.isTraceEnabled()) {
            logger.trace("Trade request from={} targetSlot={}", client.getPlayerName(), followId);
        }

        Client other = client.getClient(followId);
        if (!client.validClient(followId) || client.getSlot() == followId) {
            return;
        }

        PacketInteractionRequestService.handleTradeRequest(client, followId, other);
    }
}
