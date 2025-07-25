package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of RemoveIgnore (opcode 74).
 */
public class RemoveIgnoreListener implements PacketListener {

    static { PacketListenerManager.register(74, new RemoveIgnoreListener()); }

    private static final Logger logger = LoggerFactory.getLogger(RemoveIgnoreListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        long ign = buf.readLong();
        logger.debug("RemoveIgnoreListener: {} unignores {}", client.getPlayerName(), ign);
        client.removeIgnore(ign);
    }
}
