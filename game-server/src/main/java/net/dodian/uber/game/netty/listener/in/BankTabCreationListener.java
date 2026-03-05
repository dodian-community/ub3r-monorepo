package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.SendCurrentBankTab;

/**
 * Handles Mystic bank tab creation/select packet (opcode 216).
 */
public class BankTabCreationListener implements PacketListener {

    static {
        PacketListenerManager.register(216, new BankTabCreationListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        if (buf.readableBytes() < 7) {
            return;
        }

        int fromInterface = buf.readInt();
        int dragFromSlot = buf.readUnsignedShort();
        int toTab = buf.readUnsignedByte();

        if (!client.IsBanking) {
            return;
        }
        if (fromInterface < 50300 || fromInterface > 50310) {
            return;
        }

        if (toTab < 0) {
            toTab = 0;
        } else if (toTab > 10) {
            toTab = 10;
        }

        int bankSlot = client.resolveBankSlot(fromInterface, dragFromSlot);
        if (bankSlot < 0) {
            return;
        }

        client.assignBankSlotToTab(bankSlot, toTab);
    }
}
