package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.content.items.ItemDispatcher;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.model.entity.player.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inventory second-click (opcode 16) – mirrors legacy {@code ClickItem2}.
 */
public class ClickItem2Listener implements PacketListener {

    static { PacketListenerManager.register(16, new ClickItem2Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(ClickItem2Listener.class);

    // --- helpers matching Stream ---
    private static int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }
    private static int readSignedWordBigEndianA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        int val = (high << 8) | low;
        if (val > 32767) val -= 65536;
        return val;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        int itemId   = readUnsignedWordA(buf);
        int itemSlot = readSignedWordBigEndianA(buf);

        logger.debug("ClickItem2Listener: slot {} item {}", itemSlot, itemId);

        if (itemSlot < 0 || itemSlot > 28) { client.disconnected = true; return; }
        if (client.playerItems[itemSlot] - 1 != itemId) return;
        if (client.randomed || client.UsingAgility) return;

        if (ItemDispatcher.tryHandle(client, 2, itemId, itemSlot, -1)) {
            return;
        }

        String itemName = client.GetItemName(itemId);

        /* Slayer helm task reminder */
        if (itemName.startsWith("Slayer helm")) {
            SlayerTask.sendTask(client);
        }
    }
}
