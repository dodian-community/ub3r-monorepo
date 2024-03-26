package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveInterfaces;

public class ClickingStuff implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        //int interfaceID = client.getInputStream().readSignedByte(); //Do not exist!!
        if (client.IsBanking) {
            client.send(new RemoveInterfaces());
            client.IsBanking = false;
            client.checkItemUpdate();
        }
        if (client.IsShopping) {
            client.IsShopping = false;
            client.MyShopID = 0;
            client.UpdateShop = false;
            client.checkItemUpdate();
        }
        if (client.checkBankInterface) {
            client.send(new RemoveInterfaces());
            client.checkBankInterface = false;
            client.checkItemUpdate();
        }
        if (client.isPartyInterface) {
            client.send(new RemoveInterfaces());
            client.isPartyInterface = false;
            client.checkItemUpdate();
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
