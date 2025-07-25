package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.content.Skillcape;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of legacy DropItem (opcode 87).
 */
public class DropItemListener implements PacketListener {

    static { PacketListenerManager.register(87, new DropItemListener()); }

    private static final Logger logger = LoggerFactory.getLogger(DropItemListener.class);

    // --- stream helpers ---
    private static int readSignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int val = (high << 8) | low;
        if (val > 32767) val -= 0x10000;
        return val;
    }
    private static int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        int droppedItem = readSignedWordA(buf);
        /* interfaceId */ readUnsignedWordA(buf);
        int slot = readUnsignedWordA(buf);

        logger.debug("DropItemListener: item {} slot {}", droppedItem, slot);

        // Early exits for player state validations
        if (client.isLoggingOut || client.randomed || client.UsingAgility) return;

        long now = System.currentTimeMillis();
        if (now - client.lastDropTime < 600) {
            client.send(new SendMessage("You must wait a moment before dropping another item."));
            return;
        }

        if (slot < 0 || slot > 27) {
            System.out.println("Warning: Player " + client.getPlayerName() + " sent invalid drop slot: " + slot);
            return;
        }

        if ((client.playerItems[slot] - 1 != droppedItem) || client.playerItemsN[slot] < 1) return;

        // Potato special case
        if (droppedItem == 5733) {
            client.deleteItem(droppedItem, slot, 1);
            client.send(new SendMessage("A magical force removed this item from your inventory!"));
            client.lastDropTime = now;
            return;
        }

        // Block skillcapes / hoods
        boolean isHood = Server.itemManager.getName(droppedItem).contains("hood");
        Skillcape skillcape = Skillcape.getSkillCape(isHood ? droppedItem - 1 : droppedItem);
        if (skillcape != null) {
            client.send(new SendMessage("You cannot drop this valuable cape!"));
            return;
        }

        // Block max cape/hood
        String itemName = client.GetItemName(droppedItem);
        if (itemName.contains("Max cape") || itemName.contains("Max hood")) {
            client.send(new SendMessage("This cape represents mastery; you shouldn't drop it!"));
            return;
        }

        if (!client.wearing) {
            client.dropItem(droppedItem, slot);
            client.lastDropTime = now;
        }
    }
}
