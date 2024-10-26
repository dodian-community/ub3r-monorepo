package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.ShopHandler;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveInterfaces;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.party.Balloons;

public class RemoveItem implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int interfaceID = client.getInputStream().readUnsignedWordA();
        int removeSlot = client.getInputStream().readUnsignedWordA();
        int removeID = client.getInputStream().readUnsignedWordA();
        if (client.playerRights == 2) {
            client.println_debug("RemoveItem: " + removeID + " InterID: " + interfaceID + " slot: " + removeSlot);
        }
        if (interfaceID == 3322 && client.inDuel && client.canOffer) { // remove from bag to duel window
            client.stakeItem(removeID, removeSlot, 1);
        } else if (interfaceID == 6669 && client.inDuel && client.canOffer) { // remove from duel window
            client.fromDuel(removeID, removeSlot, 1);
        } else if (interfaceID == 1688) {
            if(client.hasSpace()) {
                int id = client.getEquipment()[removeSlot];
                int amount = client.getEquipmentN()[removeSlot];
                    if(client.remove(removeSlot, false))
                        client.addItem(id, amount);
                client.checkItemUpdate();
            } else client.send(
                    new SendMessage("Not enough space to unequip this item!"));
        } else if (interfaceID == 5064) { // remove from bag to bank
            if (client.IsBanking)
                client.bankItem(removeID, removeSlot, 1);
            else if (client.isPartyInterface)
                Balloons.offerItems(client, removeID, 1, removeSlot);
            client.checkItemUpdate();
        } else if (interfaceID == 5382) { // remove from bank
            client.fromBank(removeID, removeSlot, 1);
        } else if (interfaceID == 2274) { // remove from party
            Balloons.removeOfferItems(client, removeID, 1, removeSlot);
        } else if (interfaceID == 3322 && client.inTrade && client.canOffer) { // remove from bag to trade window
            client.tradeItem(removeID, removeSlot, 1);
        } else if (interfaceID == 3415 && client.inTrade && client.canOffer) { // remove from trade window
            client.fromTrade(removeID, removeSlot, 1);
        } else if (interfaceID >= 4233 && interfaceID <= 4257) {
            client.startGoldCrafting(interfaceID, removeSlot, 1);
        } else if (interfaceID == 3823) { // Show value to sell items
            if (!Server.shopping || client.tradeLocked) {
                client.send(new SendMessage(client.tradeLocked ? "You are trade locked!" : "Currently selling stuff to the store has been disabled!"));
                return;
            }
            if (Server.itemManager.getShopBuyValue(removeID) < 0 || !Server.itemManager.isTradable(removeID)) {
                client.send(
                        new SendMessage("You cannot sell " + client.GetItemName(removeID).toLowerCase() + " in this store."));
                return;
            }
            boolean IsIn = false;
            if (ShopHandler.ShopSModifier[client.MyShopID] > 1) {
                for (int j = 0; j <= ShopHandler.ShopItemsStandard[client.MyShopID]; j++)
                    if (removeID == (ShopHandler.ShopItems[client.MyShopID][j] - 1)) {
                        IsIn = true;
                        break;
                    }
            } else IsIn = true;
            if (!IsIn && (ShopHandler.ShopBModifier[client.MyShopID] == 2 && !ShopHandler.findDefaultItem(client.MyShopID, removeID))) {
                client.send(new SendMessage("You cannot sell " + client.GetItemName(removeID).toLowerCase() + " in this store."));
            } else {
                int currency = client.MyShopID == 55 ? 11997 : 995;
                int ShopValue = client.MyShopID == 55 ? 1000 : (int) Math.floor(client.GetShopBuyValue(removeID));
                String ShopAdd = "";
                int thousand = (int)Math.pow(10, 3);
                int million = (int)Math.pow(10, 6);
                if (ShopValue >= thousand && ShopValue < million) {
                    ShopAdd = " (" + (ShopValue / thousand) + "K)";
                } else if (ShopValue >= million) {
                    int leftover = ShopValue - ((ShopValue / million) * million);
                    ShopAdd = " (" + (ShopValue / 1000000) + ((leftover / 100000) > 0 ? "."+ (leftover / 100000) : "") + " million)";
                }
                client.send(
                        new SendMessage(client.GetItemName(removeID) + ": shop will buy for " + ShopValue + " " + client.GetItemName(currency).toLowerCase() + ShopAdd));
            }
        } else if (interfaceID == 3900) { // Show value to buy items
            int currency = client.MyShopID == 55 ? 11997 : 995;
            int ShopValue = client.MyShopID == 55 ? client.eventShopValues(removeSlot) : (int) Math.floor(client.GetShopSellValue(removeID));
            ShopValue = client.MyShopID >= 7 && client.MyShopID <= 11 ? (int) (ShopValue * 1.5) : ShopValue;
            String ShopAdd = "";
            int thousand = (int)Math.pow(10, 3);
            int million = (int)Math.pow(10, 6);
            ShopValue = client.MyShopID >= 9 && client.MyShopID <= 11 ? (int) (ShopValue * 1.5) : ShopValue;
            if (ShopValue >= thousand && ShopValue < million) {
                ShopAdd = " (" + (ShopValue / thousand) + "K)";
            } else if (ShopValue >= million) {
                int leftover = ShopValue - ((ShopValue / million) * million);
                ShopAdd = " (" + (ShopValue / 1000000) + ((leftover / 100000) > 0 ? "."+ (leftover / 100000) : "") + " million)";
            }
            client
                    .send(new SendMessage(client.GetItemName(removeID) + ": currently costs " + ShopValue + " " + client.GetItemName(currency).toLowerCase() + ShopAdd));
        } else if (interfaceID >= 1119 && interfaceID <= 1123) { // Smithing
            if (client.smithing[2] > 0) {
                client.smithing[4] = removeID;
                client.smithing[5] = 1;
                client.send(new RemoveInterfaces());
            } else {
                client.send(new SendMessage("Illigal Smithing !"));
            }
        }
        client.CheckGear();
    }

}
