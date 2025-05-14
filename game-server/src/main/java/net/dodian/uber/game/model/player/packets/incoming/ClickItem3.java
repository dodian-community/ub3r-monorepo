package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.quests.QuestSend;

public class ClickItem3 implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        client.getInputStream().readSignedWord(); //Interface id!
        int itemSlot = client.getInputStream().readUnsignedWordBigEndian();
        int itemId = client.getInputStream().readSignedWordA();
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
                client.checkItemUpdate();
                client.send(new SendMessage("you disassemble the "+client.GetItemName(itemId).toLowerCase()+"."));
            }
        }
        if (itemId == 1921 || itemId == 4456) {
            client.deleteItem(itemId, itemSlot, 1);
            client.addItemSlot(1923, 1, itemSlot);
            client.checkItemUpdate();
        }
        if (itemId >= 4458 && itemId <= 4482) {
            client.deleteItem(itemId, itemSlot, 1);
            client.addItemSlot(1980, 1, itemSlot);
            client.checkItemUpdate();
        }
        if (itemId == 1783 || itemId == 1927 || itemId == 1929 || itemId == 4286 || itemId == 4687) { //bucket empty!
            client.deleteItem(itemId, itemSlot, 1);
            client.addItemSlot(1925, 1, itemSlot);
            client.checkItemUpdate();
            if(itemId == 1927) { //Bucket of milk
                client.requestAnim(0x33D, 0);
                client.send(new SendMessage("You drank the milk and gained 15% magic penetration!"));
            }
        }
        if(itemId == 4155)
            QuestSend.showMonsterLog(client);
    }

}
