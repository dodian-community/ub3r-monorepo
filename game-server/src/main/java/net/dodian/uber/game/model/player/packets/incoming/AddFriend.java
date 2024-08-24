package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;

public class AddFriend implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        long friendtoadd = client.getInputStream().readQWord();
        client.addFriend(friendtoadd);
    }

}
