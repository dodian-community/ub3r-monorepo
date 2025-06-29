package net.dodian.uber.game.model.player.packets.incoming;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.content.Skillcape;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.networking.game.ByteBufPacket;

/**
 * ByteBuf-based replacement for the legacy DropItem packet (opcode 87).
 * Packet structure (317 protocol):
 *   1. item id (signed short, variant A)
 *   2. interface id (unsigned short, variant A) – ignored by server
 *   3. inventory slot (unsigned short, variant A)
 */
public class DropItemBuf implements ByteBufPacket {

    private static int readUnsignedShortA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }

    private static int readSignedShortA(ByteBuf buf) {
        int value = readUnsignedShortA(buf);
        return value > 32767 ? value - 0x10000 : value;
    }

    @Override
    public void process(Client client, int opcode, int size, ByteBuf payload) {
        int droppedItem = readSignedShortA(payload);
        /* int interfaceId = */ readUnsignedShortA(payload); // not used
        int slot = readUnsignedShortA(payload);

        // Prevent actions if logging out, doing agility, or in a random event
        if (client.isLoggingOut || client.randomed || client.UsingAgility) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - client.lastDropTime < 600) {
            client.send(new SendMessage("You must wait a moment before dropping another item."));
            return;
        }

        if (slot < 0 || slot > 27) {
            System.out.println("Warning: Player " + client.getPlayerName() + " sent invalid drop slot: " + slot);
            return;
        }

        if (client.playerItems[slot] - 1 != droppedItem || client.playerItemsN[slot] < 1) {
            return; // mismatch
        }

        // potato id special case
        if (droppedItem == 5733) {
            client.deleteItem(droppedItem, slot, 1);
            client.send(new SendMessage("A magical force removed this item from your inventory!"));
            client.lastDropTime = now;
            return;
        }

        // Prevent dropping skillcapes / hoods
        boolean isHood = Server.itemManager.getName(droppedItem).contains("hood");
        Skillcape skillcape = Skillcape.getSkillCape(isHood ? droppedItem - 1 : droppedItem);
        if (skillcape != null) {
            client.send(new SendMessage("You cannot drop this valuable cape!"));
            return;
        }

        // Prevent dropping max cape/hood
        if (client.GetItemName(droppedItem).contains("Max cape") || client.GetItemName(droppedItem).contains("Max hood")) {
            client.send(new SendMessage("This cape represents mastery; you shouldn't drop it!"));
            return;
        }

        if (!client.wearing) {
            client.dropItem(droppedItem, slot);
            client.lastDropTime = now;
        }
    }
}
