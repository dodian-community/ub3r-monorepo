package net.dodian.uber.game.model.player.packets.incoming;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.networking.game.ByteBufPacket;

import java.util.Date;

/**
 * ByteBuf-based handler for picking up a ground item (opcode 236).
 */
public class PickUpGroundItemBuf implements ByteBufPacket {

    private static int readSignedShortBE(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int val = (high << 8) | low;
        return val > 32767 ? val - 0x10000 : val;
    }

    private static int readUnsignedShortBE(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        return (high << 8) | low;
    }

    @Override
    public void process(Client client, int opcode, int size, ByteBuf payload) {
        int itemY = readSignedShortBE(payload);
        int itemID = payload.readUnsignedShort();
        int itemX = readSignedShortBE(payload);

        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (itemID >= 5509 && itemID <= 5515 && client.checkItem(itemID)) {
            client.send(new SendMessage("You already got this item!"));
            return;
        }
        if (itemID == 7927 && new Date().before(new Date("06/1/2024")) && client.checkItem(7927)) {
            client.send(new SendMessage("You already got this ring! Wait until after May!"));
            return;
        }
        if (System.currentTimeMillis() - client.lastAction <= 600 || (client.attemptGround != null && client.attemptGround.id == itemID)) {
            return;
        }
        client.lastAction = System.currentTimeMillis();
        client.attemptGround = Ground.findGroundItem(itemID, itemX, itemY, client.getPosition().getZ());
        if (client.getPosition().getX() != itemX || client.getPosition().getY() != itemY) {
            client.pickupWanted = true;
        } else {
            client.pickUpItem(itemX, itemY);
        }
    }
}
