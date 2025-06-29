package net.dodian.uber.game.model.player.packets.incoming;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.networking.game.ByteBufPacket;

/**
 * Netty ByteBuf-based handler for the "remove friend" packet (opcode 215).
 */
public class RemoveFriendBuf implements ByteBufPacket {
    @Override
    public void process(Client client, int opcode, int size, ByteBuf payload) {
        long friendToRemove = payload.readLong();
        client.removeFriend(friendToRemove);
    }
}
