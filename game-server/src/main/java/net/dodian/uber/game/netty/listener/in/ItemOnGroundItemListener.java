package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
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

    private static int readSignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) value -= 65536;
        return value;
    }

    private static int readUnsignedWordA(ByteBuf buf) {
        int value = ((buf.readUnsignedByte() - 128) & 0xFF) | (buf.readUnsignedByte() << 8);
        return value & 0xFFFF;
    }

    private static int readUnsignedWordBigEndianA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        return ((high << 8) | low) & 0xFFFF;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        int unknown1 = readSignedWordBigEndian(buf); // interface id of item
        int unknown2 = readUnsignedWordA(buf);       // item in bag id
        int floorID = buf.readUnsignedByte();
        int floorY = readUnsignedWordA(buf);
        int unknown3 = readUnsignedWordBigEndianA(buf);
        int floorX = buf.readUnsignedByte();

        if (logger.isTraceEnabled()) {
            logger.trace("ItemOnGroundItem interfaceId={} itemId={} floorId={} floorX={} floorY={} unknown3={} player={}",
                    unknown1, unknown2, floorID, floorX, floorY, unknown3, client.getPlayerName());
        }
        // No further behaviour in legacy implementation.
    }
}
