package net.dodian.uber.game.network.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.OutgoingPacket;

public class SendString implements OutgoingPacket {

    private String string;
    private int lineId;

    public SendString(String string, int lineId) {
        this.string = string;
        this.lineId = lineId;
    }

    @Override
    public void send(Client client) {
        client.getOutputStream().createFrameVarSizeWord(126);
        client.getOutputStream().writeString(string);
        client.getOutputStream().writeWordA(lineId);
        client.getOutputStream().endFrameVarSizeWord();
    }

}
