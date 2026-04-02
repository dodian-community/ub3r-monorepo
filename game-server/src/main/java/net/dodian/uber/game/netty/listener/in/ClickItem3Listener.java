package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.systems.content.items.ItemDispatcher;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
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
    private static final int MIN_PAYLOAD_BYTES = 6;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        int interfaceId = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.NORMAL);
        int itemSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
        int itemId = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);

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
        }
    }
}
