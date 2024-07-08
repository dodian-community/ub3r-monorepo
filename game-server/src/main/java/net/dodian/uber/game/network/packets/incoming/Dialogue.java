package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.Packet;
import net.dodian.uber.game.network.packets.outgoing.RemoveInterfaces;

public class Dialogue implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        if (client.NpcDialogue == 1 || client.NpcDialogue == 3 || client.NpcDialogue == 5 || client.NpcDialogue == 21) {
            client.NpcDialogue += 1;
            client.NpcDialogueSend = false;
        } else if (client.NpcDialogue == 6 || client.NpcDialogue == 7) {
            client.NpcDialogue = 0;
            client.NpcDialogueSend = false;
            client.send(new RemoveInterfaces());
        } else if (client.NpcDialogue == 23) {
            client.NpcDialogue += 2;
            client.NpcDialogueSend = false;
        } else if (client.nextDiag > 0) {
            client.NpcDialogue = client.nextDiag;
            client.NpcDialogueSend = false;
            client.nextDiag = -1;
        } else {
            if(client.NpcDialogue != 48054)
                client.send(new RemoveInterfaces());
        }
    }

}
