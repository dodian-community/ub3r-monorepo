package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.SendFrame27;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the first part of an "X" withdraw/deposit (opcode 135).
 * Captures slot / interface / item id and prompts client for an amount.
 */
public class BankX1Listener implements PacketListener {

    static { PacketListenerManager.register(135, new BankX1Listener()); }

    private static final Logger logger = LoggerFactory.getLogger(BankX1Listener.class);

    private static int readSignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int val = (high << 8) | low;
        if (val > 32767) val -= 65536;
        return val;
    }

    private static int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        int slot        = readSignedWordBigEndian(buf);
        int interfaceId = readUnsignedWordA(buf);
        int itemId      = readSignedWordBigEndian(buf);

        client.XremoveSlot  = slot;
        client.XinterfaceID = interfaceId;
        client.XremoveID    = itemId;

        if (logger.isTraceEnabled()) {
            logger.trace("BankX1 slot={} interface={} item={} player={}", slot, interfaceId, itemId, client.getPlayerName());
        }

        // Prompt the client for an amount (frame 27 in legacy protocol)
        client.send(new SendFrame27());
    }
}
