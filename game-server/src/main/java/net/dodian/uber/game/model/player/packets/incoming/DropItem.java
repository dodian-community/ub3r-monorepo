package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

public class DropItem implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int droppedItem = client.getInputStream().readUnsignedWordA();
        client.getInputStream().readUnsignedByte();
        client.getInputStream().readUnsignedByte();
        int slot = client.getInputStream().readUnsignedWordA();
        if (droppedItem == 5733) {
            client.deleteItem(droppedItem, slot, 1);
            client.send(new SendMessage("A magical force removed this item from your inventory!"));
            return;
        }
        if (droppedItem == 1927) {
            client.deleteItem(1927, slot, 1);
            client.addItemSlot(1925, 1, slot);
            client.requestAnim(0x33D, 0);
            client.animationReset = System.currentTimeMillis() + 800;
            client.send(new SendMessage("You drank the milk and gained 15% magic penetration!"));
            return;
        }
        if (client.wearing == false) {
            client.dropItem(droppedItem, slot);
        }
    }

}
