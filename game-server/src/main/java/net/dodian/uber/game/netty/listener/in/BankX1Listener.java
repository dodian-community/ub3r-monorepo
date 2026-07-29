package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.engine.systems.skills.SmithingSmeltingBridge;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.engine.systems.net.PacketBankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the first part of an "X" withdraw/deposit (opcode 135).
 * Decodes context then delegates to PacketBankingService.handleXPromptDecoded.
 */
@net.dodian.uber.game.netty.listener.PacketHandler(opcodes = {135})
public class BankX1Listener implements PacketListener {
    private static final Logger logger = LoggerFactory.getLogger(BankX1Listener.class);
    private static final int MIN_PAYLOAD_BYTES = 6;

    @Override
    public void handle(Client client, GamePacket packet) {
        net.dodian.uber.game.netty.game.decode.TarnishPackets.BankPresetAction msg =
                net.dodian.uber.game.netty.game.decode.TarnishPackets.BankPresetAction.decode(packet.opcode(), packet.payload());
        if (msg == null) {
            return;
        }
        int slot = msg.slot();
        int interfaceId = msg.interfaceId();
        int itemId = msg.itemId();

        if (logger.isTraceEnabled()) {
            logger.trace("BankX1 slot={} interface={} item={} player={}", slot, interfaceId, itemId, client.getPlayerName());
        }

        if (SmithingSmeltingBridge.isFurnaceFrame(interfaceId)) {
            logger.warn("Smelting interface item click amount=X-prompt interfaceId={} itemId={} slot={} player={}",
                    interfaceId, itemId, slot, client.getPlayerName());
        }

        PacketBankingService.handleXPromptDecoded(client, interfaceId, slot, itemId);
    }
}
