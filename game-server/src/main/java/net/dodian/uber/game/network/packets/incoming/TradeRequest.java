package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.network.packets.Packet;
import net.dodian.uber.game.network.packets.outgoing.SendMessage;

public class TradeRequest implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int tw = client.getInputStream().readUnsignedWord();
        Client other = client.getClient(tw);
        if (!client.validClient(tw)) {
            return;
        }
        if (client.getEquipment()[Equipment.Slot.WEAPON.getId()] == 4566) {
            client.facePlayer(tw);
            client.requestAnim(1833, 0);
            return;
        }
        if(client.isBusy() || other.isBusy()) {
            client.send(new SendMessage(client.isBusy() ? "You are currently busy" : other.getPlayerName() + " is currently busy!"));
            return;
        }
        if (!client.inTrade) {
            // client.trade_reqId = tw;
            // client.tradeReq(client.trade_reqId);
            client.duelReq(tw);
        }
    }

}
