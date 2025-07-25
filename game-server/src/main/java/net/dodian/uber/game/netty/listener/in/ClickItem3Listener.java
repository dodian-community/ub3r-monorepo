package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.model.player.quests.QuestSend;
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

        /* interfaceId */ readSignedWord(buf);
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

        if (itemId == 11864 || itemId == 11865) {
            int needed = 8 - client.freeSlots();
            if (needed > 0) {
                client.send(new SendMessage("you need " + needed + " empty inventory slots to disassemble the " + client.GetItemName(itemId).toLowerCase() + "."));
            } else {
                client.deleteItem(itemId, 1);
                client.addItem(itemId == 11865 ? 11784 : 8921, 1);
                client.addItem(4155, 1);
                client.addItem(4156, 1);
                client.addItem(4164, 1);
                client.addItem(4166, 1);
                client.addItem(4168, 1);
                client.addItem(4551, 1);
                client.addItem(6720, 1);
                client.addItem(8923, 1);
                client.checkItemUpdate();
                client.send(new SendMessage("you disassemble the " + client.GetItemName(itemId).toLowerCase() + "."));
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
        if (itemId == 1783 || itemId == 1927 || itemId == 1929 || itemId == 4286 || itemId == 4687) {
            client.deleteItem(itemId, itemSlot, 1);
            client.addItemSlot(1925, 1, itemSlot);
            client.checkItemUpdate();
            if (itemId == 1927) {
                client.requestAnim(0x33D, 0);
                client.send(new SendMessage("You drank the milk and gained 15% magic penetration!"));
            }
        }
        if (itemId == 4155) QuestSend.showMonsterLog(client);

    }
}
