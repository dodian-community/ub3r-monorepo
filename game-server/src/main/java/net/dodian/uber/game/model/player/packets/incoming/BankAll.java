package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.party.Balloons;

public class BankAll implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int removeSlot = client.getInputStream().readUnsignedWordA();
        int interfaceID = client.getInputStream().readUnsignedWord();
        int removeID = client.getInputStream().readUnsignedWordA();
        boolean stack = Server.itemManager.isStackable(removeID);

        if (interfaceID == 5064) { // remove from bag to bank
            if (client.IsBanking)
                client.bankItem(removeID, removeSlot, stack ? client.playerItemsN[removeSlot] : client.getInvAmt(removeID));
            else if (client.isPartyInterface)
                Balloons.offerItems(client, removeID, stack ? client.playerItemsN[removeSlot] : client.getInvAmt(removeID), removeSlot);
            client.checkItemUpdate();
        } else if (interfaceID == 5382) { // remove from bank
            client.fromBank(removeID, removeSlot, -2);
        } else if (interfaceID == 2274) { // remove from party
            Balloons.removeOfferItems(client, removeID, !stack ? 8 : Integer.MAX_VALUE, removeSlot);
        } else if (interfaceID == 3322 && client.inTrade) { // remove from bag to trade window
            client.tradeItem(removeID, removeSlot, stack ? client.playerItemsN[removeSlot] : client.getInvAmt(removeID));
        } else if (interfaceID == 3322 && client.inDuel) { // remove from bag to duel window
            client.stakeItem(removeID, removeSlot, stack ? client.playerItemsN[removeSlot] : client.getInvAmt(removeID));
        } else if (interfaceID == 6669 && client.inDuel) { // remove from duel window
            client.fromDuel(removeID, removeSlot, stack ? client.offeredItems.get(removeSlot).getAmount() : 28);
        } else if (interfaceID == 3415 && client.inTrade) { // remove from trade window
            client.fromTrade(removeID, removeSlot, stack ? client.offeredItems.get(removeSlot).getAmount() : 28);
        } else if (interfaceID == 3823) { // Show value to sell items
            if(client.playerRights < 2) {
                client.sellItem(removeID, removeSlot, 10);
            } else {
                client.getOutputStream().createFrame(27);
                client.XinterfaceID = interfaceID;
                client.XremoveID = removeID;
                client.XremoveSlot = removeSlot;
            }
        } else if (interfaceID == 3900) { // Show value to buy items
            client.getOutputStream().createFrame(27);
            client.XinterfaceID = interfaceID;
            client.XremoveID = removeID;
            client.XremoveSlot = removeSlot;
        }

    }

}
