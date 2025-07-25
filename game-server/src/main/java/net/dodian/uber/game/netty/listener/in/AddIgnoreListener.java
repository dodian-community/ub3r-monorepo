package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of AddIgnore (opcode 133).
 */
public class AddIgnoreListener implements PacketListener {

    static { PacketListenerManager.register(133, new AddIgnoreListener()); }

    private static final Logger logger = LoggerFactory.getLogger(AddIgnoreListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        long ig = buf.readLong();
        logger.debug("AddIgnoreListener: {} ignores {}", client.getPlayerName(), ig);
        client.addIgnore(ig);
    }
}
