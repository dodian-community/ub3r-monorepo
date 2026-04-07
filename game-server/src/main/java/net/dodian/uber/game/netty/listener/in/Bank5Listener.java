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
 * Netty implementation of legacy Bank5 packet (opcode 117).
 * Decodes packet data then delegates to PacketBankingService.handleFixedAmountDecoded.
 */
public class Bank5Listener implements PacketListener {

    static { PacketListenerManager.register(117, new Bank5Listener()); }

    private static final int MIN_PAYLOAD_BYTES = 8;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        int interfaceId = ByteBufReader.readInt(buf);
        int removeId = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.ADD);
        int removeSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);

        PacketBankingService.handleFixedAmountDecoded(client, interfaceId, removeId, removeSlot, 5);
    }
}
