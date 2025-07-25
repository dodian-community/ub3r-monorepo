package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.*;

public class SendString implements OutgoingPacket {

    private String string;
    private int lineId;

    public SendString(String string, int lineId) {
        this.string = string;
        this.lineId = lineId;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(126, MessageType.VAR_SHORT);
        message.putString(string);
        message.putShort(lineId, ValueType.ADD);
        client.send(message);
       // System.out.println("SendString: " + string + ", " + lineId);
    }

}
