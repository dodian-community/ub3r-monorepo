package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.content.skills.smithing.SmithingInterface;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.systems.net.PacketBankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the first part of an "X" withdraw/deposit (opcode 135).
 * Decodes context then delegates to PacketBankingService.handleXPromptDecoded.
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

        int interfaceId = ByteBufReader.readInt(buf);
        int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
        int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);

        if (logger.isTraceEnabled()) {
            logger.trace("BankX1 slot={} interface={} item={} player={}", slot, interfaceId, itemId, client.getPlayerName());
        }

        if (SmithingInterface.isSmeltingInterfaceFrame(interfaceId)) {
            logger.warn("Smelting interface item click amount=X-prompt interfaceId={} itemId={} slot={} player={}",
                    interfaceId, itemId, slot, client.getPlayerName());
        }

        PacketBankingService.handleXPromptDecoded(client, interfaceId, slot, itemId);
    }
}
