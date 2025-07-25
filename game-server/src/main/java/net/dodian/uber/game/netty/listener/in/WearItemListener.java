package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation for opcode 41 (wear/equip item).
 * Ports the full functionality of legacy WearItem.ProcessPacket.
 */
public class WearItemListener implements PacketListener {

    static { PacketListenerManager.register(41, new WearItemListener()); }

    private static final Logger logger = LoggerFactory.getLogger(WearItemListener.class);

    // ---- stream helpers ----
    private static int readUnsignedWord(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low  = buf.readUnsignedByte();
        return (high << 8) | low;
    }
    private static int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low  = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        int wearId  = readUnsignedWord(buf);
        int wearSlot = readUnsignedWordA(buf);
        int interfaceId = readUnsignedWordA(buf);

        logger.debug("WearItemListener: item {} slot {} interface {}", wearId, wearSlot, interfaceId);

        if (client.randomed || client.UsingAgility) return;

        client.wear(wearId, wearSlot, interfaceId);
    }
}
