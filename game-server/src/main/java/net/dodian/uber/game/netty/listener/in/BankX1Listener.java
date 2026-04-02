package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.content.skills.smithing.SmithingInterface;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
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
    private static final int MIN_PAYLOAD_BYTES = 8;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        // Mystic sends (ItemContainerOption5):
        // int   interfaceId (putInt)
        // short slot   (writeUnsignedWordBigEndian)
        // short nodeId (writeUnsignedWordBigEndian)
        int interfaceId = ByteBufReader.readInt(buf);
        int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
        int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);

        if ((interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310)) && client.bankStyleViewOpen) {
            return;
        }

        if (interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310)) {
            itemId = client.resolveBankItemId(interfaceId, slot, itemId);
            slot = client.resolveBankSlot(interfaceId, slot);
            if (slot < 0) {
                return;
            }
        }

        client.XremoveSlot  = slot;
        client.XinterfaceID = interfaceId;
        client.XremoveID    = itemId;

        if (logger.isTraceEnabled()) {
            logger.trace("BankX1 slot={} interface={} item={} player={}", slot, interfaceId, itemId, client.getPlayerName());
        }

        if (SmithingInterface.isSmeltingInterfaceFrame(interfaceId)) {
            logger.warn("Smelting interface item click amount=X-prompt interfaceId={} itemId={} slot={} player={}",
                    interfaceId, itemId, slot, client.getPlayerName());
        }

        // Accept mystic bank tab containers (50300-50310) in addition to legacy 5382
        if (interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310) ||
            interfaceId == 5064 || interfaceId == 3322 || interfaceId == 3415 ||
            interfaceId == 6669 || interfaceId == 2274 || interfaceId == 3900 || interfaceId == 3823 ||
            (interfaceId >= 4233 && interfaceId <= 4257) || SmithingInterface.isSmeltingInterfaceFrame(interfaceId)) {
            // Prompt the client for an amount (frame 27 in legacy protocol)
            client.send(new SendFrame27());
        }
    }
}
