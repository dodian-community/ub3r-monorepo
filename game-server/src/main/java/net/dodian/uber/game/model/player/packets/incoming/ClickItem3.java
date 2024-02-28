package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.quests.QuestSend;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask;
import net.dodian.utilities.Misc;

public class ClickItem3 implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        client.getInputStream().readSignedWord(); //Interface id!
        int itemSlot = client.getInputStream().readUnsignedWordBigEndian();
        int itemId = client.getInputStream().readSignedWordA();
        if (client.playerItems[itemSlot] - 1 != itemId) {
            return;
        }
        if(itemId == 11864 || itemId == 11865) {
            if(client.freeSlots() < 8)
                client.send(new SendMessage("you need " + (8 - client.freeSlots()) + " empty inventory slots to disassemble the "+client.GetItemName(itemId).toLowerCase()+"."));
            else {
                client.deleteItem(itemId, 1);
                client.addItem(itemId == 11865 ? 11784 : 8921, 1); //Imbue : not Imbue
                client.addItem(4155, 1);
                client.addItem(4156, 1);
                client.addItem(4164, 1);
                client.addItem(4166, 1);
                client.addItem(4168, 1);
                client.addItem(4551, 1);
                client.addItem(6720, 1);
                client.addItem(8923, 1);
                client.send(new SendMessage("you disassemble the "+client.GetItemName(itemId).toLowerCase()+"."));
            }
        }
        if(itemId == 4155)
            QuestSend.showMonsterLog(client);
    }

}
