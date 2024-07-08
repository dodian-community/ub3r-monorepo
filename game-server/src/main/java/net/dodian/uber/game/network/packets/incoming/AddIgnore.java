package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.Packet;

public class AddIgnore implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        long igtoadd = client.getInputStream().readQWord();
        client.addIgnore(igtoadd);
    }

}
