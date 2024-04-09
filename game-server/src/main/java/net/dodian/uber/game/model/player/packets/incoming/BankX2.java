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
        //client.send(new RemoveInterfaces()); //Remove interface after click enter!
        client.resetDialogueChat();
        if (EnteredAmount < 1) {
            return;
        }
        if (client.convoId == 1001) {
            Server.slots.rollDice(client, EnteredAmount);
            return;
        }
        if (client.XinterfaceID == 3838) { //Claim battlestaffs
            int amount = client.dailyReward.isEmpty() ? 0 : Integer.parseInt(client.dailyReward.get(2));
            int totalAmount = amount;
            amount = Math.min(EnteredAmount, amount);
            int coins = client.getInvAmt(995);
            amount = coins == 0 ? 0 : Math.min(amount, coins / 7000);
            if(coins < 7000)
                client.showNPCChat(3837, 597, new String[]{"You do not have enough coins to purchase one battlestaff."});
            else {
                client.deleteItem(995, amount * 7_000);
                client.addItem(1392, amount);
                client.dailyReward.set(2, (totalAmount - amount) + "");
                client.checkItemUpdate();
                client.showNPCChat(3837, 595, new String[]{"Here is " + amount + " battlestaffs for you."});
            }
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
            client.checkItemUpdate();
        } else if (client.XinterfaceID == 5382) { // remove from bank
            client.fromBank(client.bankItems[client.XremoveSlot] - 1, client.XremoveSlot, EnteredAmount);
            client.checkItemUpdate();
        } else if (client.XinterfaceID == 2274) { // remove from party
            Balloons.removeOfferItems(client, client.offeredPartyItems.get(client.XremoveSlot).getId(), EnteredAmount, client.XremoveSlot);
            client.checkItemUpdate();
        } else if (client.XinterfaceID == 3322 && client.inDuel) { // remove from bag to duel window
            client.stakeItem(client.XremoveID, client.XremoveSlot, EnteredAmount);
        } else if (client.XinterfaceID == 6669 && client.inDuel) { // remove from duel window
            client.fromDuel(client.XremoveID, client.XremoveSlot, EnteredAmount);
        } else if (client.XinterfaceID == 3900 && client.XremoveID != -1) { // Shop interface
            int id = client.XremoveID, slot = client.XremoveSlot;
            client.XremoveID = -1; client.XremoveSlot  = -1; //reset!
            client.buyItem(id, slot, EnteredAmount);
            client.checkItemUpdate();
            System.out.println("enter amt 1..." + EnteredAmount + ", " + id);
            client.send(new InventoryInterface(3824, 3822)); //Need this to close interface, yikes!
        } else if (client.XinterfaceID == 3823 && client.XremoveID != -1) { // Shop interface
            int id = client.XremoveID, slot = client.XremoveSlot;
            client.XremoveID = -1; client.XremoveSlot  = -1; //reset!
            client.sellItem(id, slot, EnteredAmount);
            client.checkItemUpdate();
            System.out.println("enter amt 2..." + EnteredAmount + ", " + id);
            client.send(new InventoryInterface(3824, 3822)); //Need this to close interface, yikes!
        } else if (client.XinterfaceID == 3322 && client.inTrade) { // remove from bag to trade window
            client.tradeItem(client.XremoveID, client.XremoveSlot, EnteredAmount);
        } else if (client.XinterfaceID == 3415 && client.inTrade) { // remove from trade window
            client.fromTrade(client.XremoveID, client.XremoveSlot, EnteredAmount);
        }
    }

}
