package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.Packet;
import net.dodian.uber.game.network.packets.outgoing.RemoveInterfaces;

public class ClickingStuff implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        //int interfaceID = client.getInputStream().readSignedByte(); //Do not exist!!
        if (client.IsBanking) {
            client.IsBanking = false;
            client.checkItemUpdate();
            client.send(new RemoveInterfaces());
        }
        if (client.isShopping()) {
            client.MyShopID = -1;
            client.checkItemUpdate();
            client.send(new RemoveInterfaces());
        }
        if (client.checkBankInterface) {
            client.checkBankInterface = false;
            client.checkItemUpdate();
            client.send(new RemoveInterfaces());
        }
        if (client.isPartyInterface) {
            client.isPartyInterface = false;
            client.checkItemUpdate();
            client.send(new RemoveInterfaces());
        }
        if (client.inDuel && !client.duelFight) {
            Client other = client.getClient(client.duel_with);
            if (other == null || !client.validClient(client.duel_with) || System.currentTimeMillis() - client.lastButton < 600) {
                return;
            }
            client.declineDuel();
            client.checkItemUpdate(); //We need this here?!
        }
        if (client.inTrade) {
            Client other = client.getClient(client.trade_reqId);
            if (other == null || !client.validClient(client.trade_reqId) || System.currentTimeMillis() - client.lastButton < 600) {
                return;
            }
            client.declineTrade();
            client.checkItemUpdate(); //We need this here?!
        }
        if(client.currentSkill >= 0) client.currentSkill = -1; //Close skillmenu interface!
    }

}
