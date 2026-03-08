package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
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
 * Opcode 25 – Item on ground item. The legacy handler only parsed the packet
 * and printed debug; there is no gameplay logic attached. We preserve the same
 * behaviour here (TRACE-level log for debugging) so the bridge can be removed.
 */
public class ItemOnGroundItemListener implements PacketListener {

    static { PacketListenerManager.register(25, new ItemOnGroundItemListener()); }

    private static final Logger logger = LoggerFactory.getLogger(ItemOnGroundItemListener.class);
    private static final int MIN_PAYLOAD_BYTES = 9;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        int unknown1 = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL); // interface id of item
        int unknown2 = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);   // item in bag id
        int floorID = buf.readUnsignedByte();
        int floorY = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
        int unknown3 = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
        int floorX = buf.readUnsignedByte();

        if (logger.isTraceEnabled()) {
            logger.trace("ItemOnGroundItem interfaceId={} itemId={} floorId={} floorX={} floorY={} unknown3={} player={}",
                    unknown1, unknown2, floorID, floorX, floorY, unknown3, client.getPlayerName());
        }
        // No further behaviour in legacy implementation.
    }
}
