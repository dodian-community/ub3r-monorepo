package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;

/**
 * Handles text syntax input (opcode 60).
 */
public class SyntaxInputListener implements PacketListener {

    static {
        PacketListenerManager.register(60, new SyntaxInputListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        if (!client.bankSearchPendingInput) {
            return;
        }

        String input = readTerminatedString(packet.getPayload()).trim();
        client.bankSearchPendingInput = false;

        if (!client.IsBanking) {
            return;
        }

        client.applyBankSearch(input);
    }

    private String readTerminatedString(ByteBuf buf) {
        StringBuilder sb = new StringBuilder();
        while (buf.isReadable()) {
            byte b = buf.readByte();
            if (b == 10 || b == 0) {
                break;
            }
            sb.append((char) b);
        }
        return sb.toString();
    }
}
