package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.content.items.ItemDispatcher;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty handler for opcode 75 (third click on inventory item).
 * Completely ports the logic from legacy ClickItem3.ProcessPacket.
 */
public class ClickItem3Listener implements PacketListener {

    static { PacketListenerManager.register(75, new ClickItem3Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(ClickItem3Listener.class);

    // ---------------- helpers -----------------
    private static int readSignedWord(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = buf.readUnsignedByte();
        int val = (high << 8) | low;
        if (val > 32767) val -= 0x10000;
        return val;
    }
    private static int readUnsignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        return (high << 8) | low;
    }
    private static int readSignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int val = (high << 8) | low;
        if (val > 32767) val -= 0x10000;
        return val;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        int interfaceId = readSignedWord(buf);
        int itemSlot = readUnsignedWordBigEndian(buf);
        int itemId = readSignedWordA(buf);

        logger.debug("ClickItem3Listener: slot {} item {}", itemSlot, itemId);

        if (itemSlot < 0 || itemSlot > 28) {
            client.disconnected = true;
            return;
        }
        if (client.playerItems[itemSlot] - 1 != itemId) {
            return;
        }
        if (client.randomed || client.UsingAgility) return;

        if (ItemDispatcher.tryHandle(client, 3, itemId, itemSlot, interfaceId)) {
            return;
        }
    }
}
