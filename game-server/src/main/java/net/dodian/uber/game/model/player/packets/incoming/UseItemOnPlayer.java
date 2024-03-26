package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.utilities.Misc;

public class UseItemOnPlayer implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        client.getInputStream().readSignedWordBigEndianA();
        int playerSlot = client.getInputStream().readSignedWord();
        int itemId = client.getInputStream().readSignedWord();
        int CrackerSlot = client.getInputStream().readSignedWordBigEndian();
        Client player = ((Client) PlayerHandler.players[playerSlot]);

        if (playerSlot < 0 || player == null || playerSlot > Constants.maxPlayers || !client.playerHasItem(itemId)) {
            return;
        }
        if (itemId == 5733) { //Potato
            client.playerPotato.clear();
            client.playerPotato.add(0, 1);
            client.playerPotato.add(1, playerSlot);
            client.playerPotato.add(2, player.dbId);
            client.playerPotato.add(3, 1);
            return;
        }
        if (itemId == 962 && player.freeSlots() > 0) {
            if (client.connectedFrom.equals(player.connectedFrom)) {
                client.send(new SendMessage("Can't use it on another player from same address!"));
                return;
            }
            client.deleteItem(itemId, CrackerSlot, 1);
            int[] hats = {1038, 1040, 1042, 1044, 1046, 1048};
            int partyHat = hats[Misc.random(hats.length - 1)];
            if (1 + Misc.random(99) <= 50) {
                client.addItemSlot(partyHat, 1, CrackerSlot);
                client.send(new SendMessage("You got a " + client.GetItemName(partyHat).toLowerCase() + " from the cracker!"));
            } else {
                player.addItem(partyHat, 1);
                client.checkItemUpdate();
                player.send(new SendMessage("You got a " + client.GetItemName(partyHat).toLowerCase() + " from " + client.getPlayerName()));
                client.send(new SendMessage(player.getPlayerName() + " got a  " + client.GetItemName(partyHat).toLowerCase() + " from the cracker!"));
            }
        } else if (itemId == 962)
            client.send(new SendMessage("Your partner need a slot free in their inventory!"));
    }

}
