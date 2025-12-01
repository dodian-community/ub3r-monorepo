package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

/**
 * Notifies the client that a friend should be removed from the friends list.
 * Uses opcode 51 to match Mystic client's PacketConstants.REMOVE_FRIEND.
 */
public class RemoveFriend implements OutgoingPacket {

    private final long name;

    public RemoveFriend(long name) {
        this.name = name;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(51, MessageType.FIXED);
        message.putLong(name);
        client.send(message);
    }
}
