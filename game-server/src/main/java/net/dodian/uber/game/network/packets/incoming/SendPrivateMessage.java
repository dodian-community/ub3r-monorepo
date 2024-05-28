package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.Packet;

public class SendPrivateMessage implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        long friendtosend = client.getInputStream().readQWord();
        byte[] pmchatText = new byte[100];
        int pmchatTextSize = (byte) (packetSize - 8);
        client.getInputStream().readBytes(pmchatText, pmchatTextSize, 0);
        client.sendPmMessage(friendtosend, pmchatText, pmchatTextSize);
    }

}
