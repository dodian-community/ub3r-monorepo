package net.dodian.uber.game.model.player.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;

public class RemoveInterfaces implements OutgoingPacket {

    @Override
    public void send(Client client) {
        client.IsBanking = false;
        client.checkBankInterface = false;
        client.currentSkill = -1;
        client.getOutputStream().createFrame(219);
        client.flushOutStream();
    }

}
