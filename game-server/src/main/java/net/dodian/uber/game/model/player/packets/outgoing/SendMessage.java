package net.dodian.uber.game.model.player.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;

public class SendMessage implements OutgoingPacket {

    private String message;

    public SendMessage(String message) {
        this.message = message;
    }

    @Override
    public void send(Client client) {
        client.getOutputStream().createFrameVarSize(253);
        client.getOutputStream().writeString(message);
        client.getOutputStream().endFrameVarSize();
    }

}
