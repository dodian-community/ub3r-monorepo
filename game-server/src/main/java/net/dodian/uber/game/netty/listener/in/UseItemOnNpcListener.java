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
import net.dodian.uber.game.engine.systems.net.PacketInteractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opcode 57 – Use item on NPC. Mirrors legacy {@code UseItemOnNpc} logic.
 */
public class UseItemOnNpcListener implements PacketListener {

    static { PacketListenerManager.register(57, new UseItemOnNpcListener()); }

    private static final Logger logger = LoggerFactory.getLogger(UseItemOnNpcListener.class);
    private static final int MIN_PAYLOAD_BYTES = 8;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int npcIndex = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
        ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD); // discarded value, matches legacy behaviour

        if (logger.isTraceEnabled()) {
            logger.trace("UseItemOnNpc item={} slot={} npcIndex={} player={}", itemId, slot, npcIndex, client.getPlayerName());
        }

        /* Validation */
        if (!PacketItemActionService.validateItemOnNpcSlot(client, slot)) return;
        if (itemId != client.playerItems[slot] - 1) return;

        PacketInteractionService.handleUseItemOnNpc(client, packet.opcode(), itemId, slot, npcIndex);
    }
}
