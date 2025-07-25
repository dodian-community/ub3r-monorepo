package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty port of legacy Trade packet (opcode 139).
 */
public class TradeListener implements PacketListener {

    static { PacketListenerManager.register(139, new TradeListener()); }

    private static final Logger logger = LoggerFactory.getLogger(TradeListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();
        int targetSlot = buf.readShortLE() & 0xFFFF; // little-endian to match legacy readSignedWordBigEndian
        Client other = client.getClient(targetSlot);
        if (!client.validClient(targetSlot)) {
            return;
        }
        if (client.inHeat() || other.inHeat()) {
            client.send(new SendMessage("It would not be a wise idea to trade with the heat in the background!"));
            return;
        }
        if (client.isBusy() || other.isBusy()) {
            client.send(new SendMessage(client.isBusy() ? "You are currently busy" : other.getPlayerName() + " is currently busy!"));
            return;
        }
        if (!client.inTrade) {
            client.trade_reqId = targetSlot;
            client.tradeReq(client.trade_reqId);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("{} sent Trade request to slot {} ({})", client.getPlayerName(), targetSlot, other.getPlayerName());
        }
    }
}
