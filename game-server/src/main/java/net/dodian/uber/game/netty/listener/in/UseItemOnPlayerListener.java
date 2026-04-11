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
 * Opcode 14 – Use item on player (e.g., party-cracker).
 * Decodes fields then delegates to PacketItemActionService.
 */
public class UseItemOnPlayerListener implements PacketListener {

    static { PacketListenerManager.register(14, new UseItemOnPlayerListener()); }

    private static final Logger logger = LoggerFactory.getLogger(UseItemOnPlayerListener.class);
    private static final int MIN_PAYLOAD_BYTES = 8;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }
        ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.ADD); // unused index – matches legacy discard
        int playerSlot = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.NORMAL);
        int itemId = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.NORMAL);
        int crackerSlot = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);

        if (logger.isTraceEnabled()) {
            logger.trace("UseItemOnPlayer item={} on playerSlot={} crackerSlot={} from={}", itemId, playerSlot, crackerSlot, client.getPlayerName());
        }

        PacketItemActionService.handleUseItemOnPlayer(client, playerSlot, itemId, crackerSlot);
    }
}
