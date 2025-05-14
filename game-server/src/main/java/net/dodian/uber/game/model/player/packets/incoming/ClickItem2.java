package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask;
import net.dodian.utilities.Misc;

public class ClickItem2 implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int itemId = client.getInputStream().readUnsignedWordA();
        int itemSlot = client.getInputStream().readSignedWordBigEndianA();
        String itemName = client.GetItemName(client.playerItems[itemSlot] - 1);
        if (client.playerItems[itemSlot] - 1 != itemId) {
            return;
        }
        if (client.randomed || client.UsingAgility) {
            return;
        }
        if(itemSlot > 28 || itemSlot < 0) { //No need to go out of scope!
            client.disconnected = true;
            return;
        }
        int slot = itemId == 5509 ? 0 : ((itemId - 5508) / 2);
        if (slot >= 0 && slot <= 3) {
            client.send(new SendMessage("There is " + client.runePouchesAmount[slot] + " rune essence in this pouch!"));
        }
        if (itemId == 13203) {
            String[] quotes = {
                    "You are easily the spunkiest warrior alive!",
                    "Not a single soul can challenge your spunk!",
                    "You are clearly the most spunktastic in all the land!",
                    "Your might is spunktacular!",
                    "It's spunkalicious!",
                    "You... are... spunky!",
                    "You are too spunktacular to measure!",
                    "You are the real M.V.P. dude!",
                    "More lazier then Spunky is Ivan :D"
            };
            client.send(new SendMessage(quotes[Misc.random(quotes.length - 1)]));
        }
        if(itemName.startsWith("Slayer helm")) {
            SlayerTask.sendTask(client);
        }
        if(itemId == 4155) { //Partner on slayer gem!
            System.out.println("Hello!");
            client.NpcDialogue = 16;
            client.NpcDialogueSend = false;
            client.nextDiag = -1;
        }
        if (itemId == 11997) {
            client.send(new SendMessage("Event is over! Will use in the future?!")); //I need to bring these back to Duke!
        }
        if (itemId == 4936) {
            client.send(new SendMessage("This crossbow need a Seercull bow to be fully repaired."));
        }
        if (itemId == 4864) {
            client.send(new SendMessage("This staff need a Master wand to be fully repaired."));
        }
        if (itemId == 4566) {
            client.requestAnim(1835, 0);
        }
    }

}
