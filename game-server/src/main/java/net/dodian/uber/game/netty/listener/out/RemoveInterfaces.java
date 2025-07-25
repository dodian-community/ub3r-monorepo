package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;

public class RemoveInterfaces implements OutgoingPacket {

    @Override
    public void send(Client client) {
        client.checkBankInterface = false;
        client.currentSkill = -1;
        ByteMessage bm = ByteMessage.message(219);
        client.send(bm);
       // System.out.println("RemoveInterfaces packet sent");
    }

}
