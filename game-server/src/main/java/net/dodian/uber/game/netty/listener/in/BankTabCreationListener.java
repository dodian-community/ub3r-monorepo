package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.engine.systems.net.PacketBankingService;

/**
 * Handles Mystic bank tab creation/select packet (opcode 216).
 * Delegates all state logic to PacketBankingService.
 */
public class BankTabCreationListener implements PacketListener {

    static {
        PacketListenerManager.register(216, new BankTabCreationListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.payload();
        if (buf.readableBytes() < 7) {
            return;
        }

        int fromInterface = buf.readInt();
        int dragFromSlot = buf.readUnsignedShort();
        int toTab = buf.readUnsignedByte();

        PacketBankingService.handleTabCreation(client, fromInterface, dragFromSlot, toTab);
    }
}
