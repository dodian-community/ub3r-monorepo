package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

public class Trade implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int temp = client.getInputStream().readSignedWordBigEndian();
        Client other = client.getClient(temp);
        if (!client.validClient(temp)) {
            return;
        }
        if (client.inHeat() || other.inHeat()) {
            client.send(new SendMessage("It would not be a wise idea to trade with the heat in the background!"));
            return;
        }
        if(client.isBusy() || other.isBusy()) {
            client.send(new SendMessage(client.isBusy() ? "You are currently busy" : other.getPlayerName() + " is currently busy!"));
            return;
        }
        if (!client.inTrade) {
            client.trade_reqId = temp;
            client.tradeReq(client.trade_reqId);
        }
    }

}
