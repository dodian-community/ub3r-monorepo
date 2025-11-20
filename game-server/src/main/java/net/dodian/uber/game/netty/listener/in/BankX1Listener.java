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

        // Mystic sends (ItemContainerOption5):
        // int   interfaceId (putInt)
        // short slot   (writeUnsignedWordBigEndian)
        // short nodeId (writeUnsignedWordBigEndian)
        int interfaceId = buf.readInt();
        int slot        = readSignedWordBigEndian(buf) & 0xFFFF;
        int itemId      = readSignedWordBigEndian(buf) & 0xFFFF;

        client.XremoveSlot  = slot;
        client.XinterfaceID = interfaceId;
        client.XremoveID    = itemId;

        if (logger.isTraceEnabled()) {
            logger.trace("BankX1 slot={} interface={} item={} player={}", slot, interfaceId, itemId, client.getPlayerName());
        }

        // Accept mystic bank tab containers (50300-50310) in addition to legacy 5382
        if (interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310) ||
            interfaceId == 5064 || interfaceId == 3322 || interfaceId == 3415 ||
            interfaceId == 6669 || interfaceId == 2274 || interfaceId == 3900 || interfaceId == 3823) {
            // Prompt the client for an amount (frame 27 in legacy protocol)
            client.send(new SendFrame27());
        }
    }
}
