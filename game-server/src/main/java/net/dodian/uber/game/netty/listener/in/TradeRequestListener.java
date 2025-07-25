package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of legacy TradeRequest packet (opcode 128).
 */
public class TradeRequestListener implements PacketListener {

    static { PacketListenerManager.register(128, new TradeRequestListener()); }

    private static final Logger logger = LoggerFactory.getLogger(TradeRequestListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        int targetSlot = buf.readUnsignedShort(); // matches readUnsignedWord
        Client other = client.getClient(targetSlot);
        if (!client.validClient(targetSlot) || client.getSlot() == targetSlot) {
            return;
        }

        // If holding rubber chicken, just do the emote.
        if (client.getEquipment()[Equipment.Slot.WEAPON.getId()] == 4566) {
            client.facePlayer(targetSlot);
            client.requestAnim(1833, 0);
            return;
        }

        if (client.isBusy() || other.isBusy()) {
            client.send(new SendMessage(client.isBusy() ? "You are currently busy" : other.getPlayerName() + " is currently busy!"));
            return;
        }

        if (!client.inTrade) {
            // Unlike legacy comment, here we always use duelReq (original code did).
            client.duelReq(targetSlot);
        }

        if (logger.isTraceEnabled()) {
            logger.trace("{} sent TradeRequest/DuelReq to slot {} ({})", client.getPlayerName(), targetSlot, other.getPlayerName());
        }
    }
}
