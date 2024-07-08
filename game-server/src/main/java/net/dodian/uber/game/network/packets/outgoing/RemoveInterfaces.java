package net.dodian.uber.game.network.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.OutgoingPacket;

public class RemoveInterfaces implements OutgoingPacket {

    @Override
    public void send(Client client) {
        client.checkBankInterface = false;
        client.currentSkill = -1;
        client.getOutputStream().createFrame(219);
        //client.flushOutStream();
    }

}
