package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.InventoryInterface;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveInterfaces;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Utils;

public class BankX2 implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int EnteredAmount = client.getInputStream().readDWord();
        if (EnteredAmount < 1) {
            return;
        }
        if (client.convoId == 1001) {
            client.send(new RemoveInterfaces());
            Server.slots.rollDice(client, EnteredAmount);
            return;
        }
        if(client.XinterfaceID == 4753) { //Herb making code
            client.send(new RemoveInterfaces());
            int slot = client.XremoveSlot - 1;
            int id = client.herbOptions.get(slot).getId();
            boolean grimy = client.GetItemName(id).toLowerCase().contains("grimy");
            int coins = client.getInvAmt(995);
            if(grimy) {
                EnteredAmount = Math.min(EnteredAmount, coins / 200);
                EnteredAmount = Math.min(EnteredAmount, client.getInvAmt(id));
                if(EnteredAmount > 0) {
                    if(client.getInvAmt(id) <= EnteredAmount) client.herbOptions.remove(slot);
                    int otherHerb = -1;
                    for(int h = 0; h < Utils.grimy_herbs.length && otherHerb == -1; h++)
                        if(id == client.GetNotedItem(Utils.grimy_herbs[h]))
                            otherHerb = client.GetNotedItem(Utils.herbs[h]);
                    client.deleteItem(995, EnteredAmount * 200);
                    client.deleteItem(id, EnteredAmount);
                    client.addItem(otherHerb, EnteredAmount);
                    client.checkItemUpdate();
                    client.showNPCChat(client.NpcTalkTo, 598, new String[]{"Here is your all of ", EnteredAmount + " " + client.GetItemName(id).toLowerCase()});
                    if(!client.herbOptions.isEmpty())
                        client.nextDiag = 4756;
                } else client.showNPCChat(client.NpcTalkTo, 605, new String[]{"You need 1 herb and 200 coins", "for me to grind it for you."});
            } else if(!client.playerHasItem(228))
                client.showNPCChat(client.NpcTalkTo, 605, new String[]{"You need noted vial of water for me to do that!"});
            else { //Make unfinish potions!
                int otherHerb = -1;
                for(int h = 0; h < Utils.herb_unf.length && otherHerb == -1; h++)
                    if(id == client.GetNotedItem(Utils.herb_unf[h]))
                        otherHerb = client.GetNotedItem(Utils.herbs[h]);
                int vials = client.getInvAmt(228), herbs = client.getInvAmt(otherHerb);
                EnteredAmount = Math.min(EnteredAmount, coins / 1_000); //Check coins first!
                EnteredAmount = Math.min(EnteredAmount, vials); //Vial after
                EnteredAmount = Math.min(EnteredAmount, herbs); //Herb last
                if(EnteredAmount > 0) {
                    if(herbs <= EnteredAmount) client.herbOptions.remove(slot);
                    client.deleteItem(995, EnteredAmount * 1_000);
                    client.deleteItem(228, EnteredAmount);
                    client.deleteItem(otherHerb, EnteredAmount);
                    client.addItem(id, EnteredAmount);
                    client.checkItemUpdate();
                    client.showNPCChat(client.NpcTalkTo, 598, new String[]{"Here is your all of ", EnteredAmount + " " + client.GetItemName(id).toLowerCase()});
                    if(!client.herbOptions.isEmpty())
                        client.nextDiag = 4757;
                } else client.showNPCChat(client.NpcTalkTo, 605, new String[]{"You need atleast 1 herb, 1 vial of water and 1000 coins", "for me to turn it into a unfinish potion."});
            }
            client.XinterfaceID = -1;
            return;
        }
        if (client.XinterfaceID == 3838) { //Claim battlestaffs
            client.send(new RemoveInterfaces());
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
            client.send(new RemoveInterfaces());
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
        } else if (client.XinterfaceID == 3322 && client.inDuel && client.canOffer) { // remove from bag to duel window
            client.stakeItem(client.XremoveID, client.XremoveSlot, EnteredAmount);
        } else if (client.XinterfaceID == 6669 && client.inDuel && client.canOffer) { // remove from duel window
            client.fromDuel(client.XremoveID, client.XremoveSlot, EnteredAmount);
        } else if (client.XinterfaceID == 3900 && client.XremoveID != -1) { // Shop interface
            int id = client.XremoveID, slot = client.XremoveSlot;
            client.XremoveID = -1; client.XremoveSlot  = -1; //reset!
            client.buyItem(id, slot, EnteredAmount);
            client.checkItemUpdate();
            client.send(new InventoryInterface(3824, 3822)); //Need this to close interface, yikes!
        } else if (client.XinterfaceID == 3823 && client.XremoveID != -1) { // Shop interface
            int id = client.XremoveID, slot = client.XremoveSlot;
            client.XremoveID = -1; client.XremoveSlot  = -1; //reset!
            client.sellItem(id, slot, EnteredAmount);
            client.checkItemUpdate();
            client.send(new InventoryInterface(3824, 3822)); //Need this to close interface, yikes!
        } else if (client.XinterfaceID == 3322 && client.inTrade && client.canOffer) { // remove from bag to trade window
            client.tradeItem(client.XremoveID, client.XremoveSlot, EnteredAmount);
        } else if (client.XinterfaceID == 3415 && client.inTrade && client.canOffer) { // remove from trade window
            client.fromTrade(client.XremoveID, client.XremoveSlot, EnteredAmount);
        }
    }

}
