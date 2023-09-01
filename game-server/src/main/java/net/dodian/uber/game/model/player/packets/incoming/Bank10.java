package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveInterfaces;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.party.Balloons;

import static net.dodian.uber.utilities.DotEnvKt.getGameWorldId;

public class Bank10 implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int interfaceID = client.getInputStream().readUnsignedWordBigEndian();
        int removeID = client.getInputStream().readUnsignedWordA();
        int removeSlot = client.getInputStream().readUnsignedWordA();
        if (getGameWorldId() > 1)
            client.println_debug("RemoveItem 10: " + removeID + " InterID: " + interfaceID + " slot: " + removeSlot);
        if (interfaceID == 3322 && client.inDuel) { // remove from bag to duel window
            client.stakeItem(removeID, removeSlot, 10);
        } else if (interfaceID == 6669 && client.inDuel) { // remove from duel window
            client.fromDuel(removeID, removeSlot, 10);
        } else if (interfaceID == 5064) { // remove from bag to bank
            if (client.IsBanking)
                client.bankItem(removeID, removeSlot, 10);
            else if (client.isPartyInterface)
                Balloons.offerItems(client, removeID, 10, removeSlot);
        } else if (interfaceID == 5382) { // remove from bank
            client.fromBank(removeID, removeSlot, 10);
        } else if (interfaceID == 2274) { // remove from party
            Balloons.removeOfferItems(client, removeID, 10, removeSlot);
        } else if (interfaceID == 3322 && client.inTrade) { // remove from bag to trade window
            client.tradeItem(removeID, removeSlot, 10);
        } else if (interfaceID == 3415 && client.inTrade) { // remove from trade window
            client.fromTrade(removeID, removeSlot, 10);
        } else if (interfaceID >= 4233 && interfaceID <= 4245) {
            client.startGoldCrafting(interfaceID, removeSlot, 10);
        } else if (interfaceID == 3823) { // Show value to sell items
            client.sellItem(removeID, removeSlot, 5);
        } else if (interfaceID == 3900) { // Show value to buy items
            client.buyItem(removeID, removeSlot, 5);
        } else if (interfaceID >= 1119 && interfaceID <= 1123) { // Smithing
            if (client.smithing[2] > 0) {
                client.smithing[4] = removeID;
                client.smithing[0] = 1;
                client.smithing[5] = client.smithing[3] != -1 ? client.getInvAmt(client.smithing[3]) : 10;
                client.send(new RemoveInterfaces());
            } else {
                client.send(new SendMessage("Illigal Smithing !"));
                client.println_debug("Illigal Smithing !");
            }
        }
    }

}
