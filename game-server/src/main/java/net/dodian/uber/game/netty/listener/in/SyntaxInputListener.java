package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.engine.systems.net.PacketBankingService;

/**
 * Handles text syntax input (opcode 60).
 * Delegates bank-search flag checks and state mutation to PacketBankingService.
 */
public class SyntaxInputListener implements PacketListener {

    static {
        PacketListenerManager.register(60, new SyntaxInputListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        if (!PacketBankingService.hasPendingBankSearch(client)) {
            return;
        }

        ByteBuf buf = packet.payload();
        if (!buf.isReadable()) {
            return;
        }

        String input = ByteBufReader.readTerminatedString(buf, 256).trim();
        PacketBankingService.applyPendingBankSearch(client, input);
    }
}
