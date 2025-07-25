package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of AddFriend (opcode 188).
 */
public class AddFriendListener implements PacketListener {

    static { PacketListenerManager.register(188, new AddFriendListener()); }

    private static final Logger logger = LoggerFactory.getLogger(AddFriendListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        long friend = buf.readLong(); // big-endian QWord
        logger.debug("AddFriendListener: {} adds {}", client.getPlayerName(), friend);
        client.addFriend(friend);
    }
}
