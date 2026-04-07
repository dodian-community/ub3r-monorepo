package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.engine.event.GameEventBus;
import net.dodian.uber.game.events.item.ItemDropEvent;
import net.dodian.uber.game.model.Position;
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

        if (slot < 0 || slot > 27) {
            return;
        }

        int dropAmount = client.playerItemsN[slot];
        Position dropPosition = client.getPosition().copy();
        GameEventBus.post(new ItemDropEvent(client, droppedItem, slot, dropAmount, dropPosition));
    }
}
