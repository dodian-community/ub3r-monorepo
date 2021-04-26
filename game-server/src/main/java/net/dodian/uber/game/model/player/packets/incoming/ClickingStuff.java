package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;

public class ClickingStuff implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        //int interfaceID = client.getInputStream().readSignedByte(); //Do not exist!!
        if (client.inDuel && !client.duelFight) {
            client.declineDuel();
        }
        if (client.inTrade && (!client.tradeConfirmed || !client.tradeConfirmed2)) {
            client.declineTrade();
        }
        if (client.IsShopping) {
            client.IsShopping = false;
            client.MyShopID = 0;
            client.UpdateShop = false;
        }
        if (client.IsBanking) {
            client.IsBanking = false;
        }
        if (client.isPartyInterface) {
            client.isPartyInterface = false;
        }
    }

}
