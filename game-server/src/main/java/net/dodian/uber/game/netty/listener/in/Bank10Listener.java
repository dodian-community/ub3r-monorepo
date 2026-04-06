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
 * Netty implementation of legacy Bank10 packet (opcode 43).
 * Decodes packet data and delegates container actions to Kotlin systems code.
 */
public class Bank10Listener implements PacketListener {

    static { PacketListenerManager.register(43, new Bank10Listener()); }

    private static final int MIN_PAYLOAD_BYTES = 8;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        int interfaceId = ByteBufReader.readInt(buf);
        int removeId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int removeSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int bankSlot = removeSlot;

        if ((interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310)) && client.bankStyleViewOpen) {
            return;
        }

        if (interfaceId == 5382 || (interfaceId >= 50300 && interfaceId <= 50310)) {
            bankSlot = client.resolveBankSlot(interfaceId, removeSlot);
            removeId = client.resolveBankItemId(interfaceId, removeSlot, removeId);
        }

        PacketBankingService.handleFixedAmount(client, interfaceId, removeId, removeSlot, bankSlot, 10);
    }
}
