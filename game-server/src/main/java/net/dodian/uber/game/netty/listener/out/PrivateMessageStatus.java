package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sent to update the private message status in the client.
 * Status values:
 * 0 = Loading
 * 1 = Connecting
 * 2 = Fine
 */
public class PrivateMessageStatus implements OutgoingPacket {

    private final int status;

    /**
     * Creates a new PrivateMessageStatus packet.
     * 
     * @param status The status to send (0 = loading, 1 = connecting, 2 = fine)
     */
    public PrivateMessageStatus(int status) {
        this.status = status;
        //System.out.println("PM STATUS: " + status);
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(221, MessageType.FIXED);
        message.put(status);
        client.send(message);
    }
}
