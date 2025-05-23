package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.InventoryInterface;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveInterfaces;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.party.Balloons;

public class BankX2 implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int EnteredAmount = client.getInputStream().readDWord();
        if (EnteredAmount < 1) {
            return;
        }
        if (client.convoId == 1001) {
            Server.slots.rollDice(client, EnteredAmount);
            return;
        }
        if (client.enterAmountId > 0) {
            if (client.enterAmountId == 1) {// cooking amt
                if (client.inTrade || client.inDuel) {
                    client.send(new SendMessage("Cannot cook in duel or trade"));
                    return;
                }
                client.cookAmount = EnteredAmount;
                client.enterAmountId = 0;
                client.cooking = true;
                return;
            }
        }
        if (client.XinterfaceID == 5064) { // remove from bag to bank
            if (client.IsBanking)
                client.bankItem(client.playerItems[client.XremoveSlot] - 1, client.XremoveSlot, EnteredAmount);
            else if (client.isPartyInterface)
                Balloons.offerItems(client, client.playerItems[client.XremoveSlot] - 1, EnteredAmount, client.XremoveSlot);
        } else if (client.XinterfaceID == 5382) { // remove from bank
            client.fromBank(client.bankItems[client.XremoveSlot] - 1, client.XremoveSlot, EnteredAmount);
        } else if (client.XinterfaceID == 2274) { // remove from party
            Balloons.removeOfferItems(client, client.offeredPartyItems.get(client.XremoveSlot).getId(), EnteredAmount, client.XremoveSlot);
        } else if (client.XinterfaceID == 3322 && client.inDuel) { // remove from bag to duel window
            client.stakeItem(client.XremoveID, client.XremoveSlot, EnteredAmount);
        } else if (client.XinterfaceID == 6669 && client.inDuel) { // remove from duel window
            client.fromDuel(client.XremoveID, client.XremoveSlot, EnteredAmount);
        } else if (client.XinterfaceID == 3900 && client.XremoveID != -1) { // Shop interface
            client.send(new InventoryInterface(3824, 3822)); //Need this to close interface, yikes!
            int id = client.XremoveID, slot = client.XremoveSlot;
            client.XremoveID = -1; client.XremoveSlot  = -1; //reset!
            client.buyItem(id, slot, EnteredAmount);
        } else if (client.XinterfaceID == 3823 && client.XremoveID != -1) { // Shop interface
            client.send(new InventoryInterface(3824, 3822)); //Need this to close interface, yikes!
            int id = client.XremoveID, slot = client.XremoveSlot;
            client.XremoveID = -1; client.XremoveSlot  = -1; //reset!
            client.sellItem(id, slot, EnteredAmount);
        } else if (client.XinterfaceID == 3322 && client.inTrade) { // remove from bag to trade window
            client.tradeItem(client.XremoveID, client.XremoveSlot, EnteredAmount);
        } else if (client.XinterfaceID == 3415 && client.inTrade) { // remove from trade window
            client.fromTrade(client.XremoveID, client.XremoveSlot, EnteredAmount);
        }
    }

}
