package net.dodian.uber.game.model.player.packets.incoming;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.networking.game.ByteBufPacket;

/**
 * Netty ByteBuf-based handler for the "add friend" packet (opcode 188).
 * The payload is an 8-byte Jagex “long username hash”.
 */
public class AddFriendBuf implements ByteBufPacket {

    @Override
    public void process(Client client, int opcode, int size, ByteBuf payload) {
        long friendToAdd = payload.readLong();
        client.addFriend(friendToAdd);
    }
}
