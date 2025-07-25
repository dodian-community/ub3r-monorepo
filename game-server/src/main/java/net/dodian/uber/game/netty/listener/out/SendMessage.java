package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;


public class SendMessage implements OutgoingPacket {

    private String message;

    public SendMessage(String message) {
        this.message = message;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(253, MessageType.VAR);
        message.putString(this.message);
        client.send(message);
    }

}
