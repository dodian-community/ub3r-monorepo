package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of RemoveFriend (opcode 215).
 */
public class RemoveFriendListener implements PacketListener {

    static { PacketListenerManager.register(215, new RemoveFriendListener()); }

    private static final Logger logger = LoggerFactory.getLogger(RemoveFriendListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        long friend = buf.readLong(); // big-endian qword
        logger.debug("RemoveFriendListener: {} removes {}", client.getPlayerName(), friend);
        client.removeFriend(friend);
    }
}
