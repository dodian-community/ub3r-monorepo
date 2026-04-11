package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.engine.systems.net.PacketBankingService;

/**
 * Netty port of legacy BankAll packet (opcode 129).
 * Decodes packet fields, then delegates to PacketBankingService.handleBankAllDecoded.
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

        PacketBankingService.handleBankAllDecoded(client, interfaceId, removeSlot, removeId);
    }
}
