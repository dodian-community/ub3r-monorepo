package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.engine.systems.net.PacketItemActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation for opcode 41 (wear/equip item).
 * Decodes packet fields and delegates to PacketItemActionService.
 */
public class WearItemListener implements PacketListener {

    static { PacketListenerManager.register(41, new WearItemListener()); }

    private static final Logger logger = LoggerFactory.getLogger(WearItemListener.class);
    private static final int MIN_PAYLOAD_BYTES = 6;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        int wearId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
        int wearSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);

        logger.debug("WearItemListener: item {} slot {} interface {}", wearId, wearSlot, interfaceId);

        PacketItemActionService.handleWear(client, wearId, wearSlot, interfaceId);
    }
}

