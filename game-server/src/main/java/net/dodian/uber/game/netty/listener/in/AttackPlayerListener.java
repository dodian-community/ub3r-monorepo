package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.engine.systems.net.PacketInteractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opcode 73 – player attacking another player.
 */
public class AttackPlayerListener implements PacketListener {

    static { PacketListenerManager.register(73, new AttackPlayerListener()); }

    private static final Logger logger = LoggerFactory.getLogger(AttackPlayerListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < 2) {
            return;
        }
        int victimSlot = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);

        if (logger.isTraceEnabled()) {
            logger.trace("AttackPlayer from={} victimSlot={}", client.getPlayerName(), victimSlot);
        }

        PacketInteractionService.handleAttackPlayer(client, packet.opcode(), victimSlot);
    }
}
