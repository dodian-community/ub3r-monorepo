package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Outgoing packet 196 – sends a private message to another player.
 */
public class PrivateMessage implements OutgoingPacket {

    private final long recipient;
    private final int rights;
    private final byte[] message;
    private final int size;
    private final int messageId;

    public PrivateMessage(long recipient, int rights, byte[] message, int size, int messageId) {
        this.recipient = recipient;
        this.rights = rights;
        this.message = message;
        this.size = size;
        this.messageId = messageId;
    }

    @Override
    public void send(Client client) {
        System.out.println("Send private message: " + recipient);
        ByteMessage msg = ByteMessage.message(196, MessageType.VAR);
        msg.putLong(recipient);
        msg.putInt(messageId);
        msg.put(rights);
        for (int i = 0; i < size; i++) {
            msg.put(message[i]);
        }
        client.send(msg);
    }
}
