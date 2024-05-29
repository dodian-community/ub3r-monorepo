package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.Packet;

public class WearItem implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int wearID = client.getInputStream().readUnsignedWord();
        int wearSlot = client.getInputStream().readUnsignedWordA();
        int interfaceID = client.getInputStream().readUnsignedWordA();
        if (client.randomed || client.UsingAgility) {
            return;
        }
        client.wear(wearID, wearSlot, interfaceID);
    }

}
