package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.network.packets.Packet;
import net.dodian.uber.game.network.packets.outgoing.SendMessage;

import java.util.Date;

public class PickUpGroundItem implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int itemY = client.getInputStream().readSignedWordBigEndian();
        int itemID = client.getInputStream().readUnsignedWord();
        int itemX = client.getInputStream().readSignedWordBigEndian();
        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (itemID >= 5509 && itemID <= 5515 && client.checkItem(itemID)) {
            client.send(new SendMessage("You already got this item!"));
            return;
        }
        if(itemID == 7927 && new Date().before(new Date("06/1/2024")) && client.checkItem(7927)) {
            client.send(new SendMessage("You already got this ring! Wait until after May!"));
            return;
        }
        if (System.currentTimeMillis() - client.lastAction <= 600 || (client.attemptGround != null && client.attemptGround.id == itemID)) {
            return;
        }
        client.lastAction = System.currentTimeMillis();
        client.attemptGround = Ground.findGroundItem(itemID, itemX, itemY, client.getPosition().getZ());
        if (client.getPosition().getX() != itemX || client.getPosition().getY() != itemY)
            client.pickupWanted = true;
        else client.pickUpItem(itemX, itemY);
    }

}
