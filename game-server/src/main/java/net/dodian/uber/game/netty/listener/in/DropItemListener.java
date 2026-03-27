package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.content.Skillcape;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
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
    private static final int MIN_PAYLOAD_BYTES = 6;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }
        int droppedItem = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);
        ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);

        logger.debug("DropItemListener: item {} slot {}", droppedItem, slot);

        // Early exits for player state validations
        if (client.isLoggingOut || client.randomed || client.UsingAgility) return;

        long now = System.currentTimeMillis();
        if (now - client.lastDropTime < 600) {
            client.send(new SendMessage("You must wait a moment before dropping another item."));
            return;
        }

        if (slot < 0 || slot > 27) {
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
