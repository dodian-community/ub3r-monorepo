package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.net.PacketBankingService;

/**
 * Netty version of legacy {@code RemoveItem} (opcode 145).
 * Decodes packet fields then delegates to PacketBankingService.handleRemoveItemDecoded.
 */
@PacketHandler(opcode = 145)
public class RemoveItemListener implements PacketListener {

    static {
        PacketListenerManager.register(145, new RemoveItemListener());
    }

    private static final int MIN_PAYLOAD_BYTES = 8;

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < MIN_PAYLOAD_BYTES) {
            return;
        }

        int interfaceID = ByteBufReader.readInt(buf);
        int removeSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
        int removeID = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);

        PacketBankingService.handleRemoveItemDecoded(client, interfaceID, removeSlot, removeID);
    }
}
