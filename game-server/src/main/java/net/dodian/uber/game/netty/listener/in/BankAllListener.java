package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.net.PacketBankingService;

/**
 * Netty port of legacy BankAll packet (opcode 129).
 * The listener now only decodes and resolves container context before delegating.
 */
public class BankAllListener implements PacketListener {

    static { PacketListenerManager.register(129, new BankAllListener()); }

    private static final int MIN_PAYLOAD_BYTES = 8;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        int removeSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int interfaceId = ByteBufReader.readInt(buf);
        int removeId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int bankSlot = removeSlot;

        if ((interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310)) && client.bankStyleViewOpen) {
            return;
        }

        int resolvedItemId = removeId;
        if (interfaceId == 5064) {
            if (removeSlot >= 0 && removeSlot < client.playerItems.length && client.playerItems[removeSlot] > 0) {
                resolvedItemId = client.playerItems[removeSlot] - 1;
            }
        } else if (interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310)) {
            bankSlot = client.resolveBankSlot(interfaceId, removeSlot);
            if (bankSlot >= 0 && bankSlot < client.bankItems.length && client.bankItems[bankSlot] > 0) {
                resolvedItemId = client.bankItems[bankSlot] - 1;
            }
        }

        PacketBankingService.handleBankAll(client, interfaceId, removeSlot, removeId, bankSlot, resolvedItemId);
    }
}
