package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.net.PacketMagicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of legacy MagicOnItems (opcode 237).
 * Decodes slot/item/spell then delegates to PacketMagicService.
 */
@PacketHandler(opcode = 237)
public class MagicOnItemsListener implements PacketListener {

    static {
        PacketListenerManager.register(237, new MagicOnItemsListener());
    }

    private static final Logger logger = LoggerFactory.getLogger(MagicOnItemsListener.class);
    private static final int MIN_PAYLOAD_BYTES = 8;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }
        int castOnSlot = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.NORMAL);
        int castOnItem = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.NORMAL); // unused / interface id
        int castSpell = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);

        PacketMagicService.handleMagicOnItem(client, castOnSlot, castOnItem, castSpell);
    }
}
