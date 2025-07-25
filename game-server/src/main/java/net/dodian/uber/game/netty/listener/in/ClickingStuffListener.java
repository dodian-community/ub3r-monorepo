package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty implementation of the miscellaneous interface-click packet (opcode 130).
 * Mirrors the full behaviour of legacy {@code ClickingStuff.ProcessPacket}.
 */
public class ClickingStuffListener implements PacketListener {

    static { PacketListenerManager.register(130, new ClickingStuffListener()); }

    private static final Logger logger = LoggerFactory.getLogger(ClickingStuffListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        // The original handler ignored the payload (a signed byte). We just advance if present.
        ByteBuf buf = packet.getPayload();
        if (buf.isReadable()) buf.readByte();

        logger.debug("ClickingStuffListener triggered for player {}", client.getPlayerName());

        if (client.IsBanking) {
            client.IsBanking = false;
            client.checkItemUpdate();
            client.send(new RemoveInterfaces());
        }
        if (client.isShopping()) {
            client.MyShopID = -1;
            client.checkItemUpdate();
            client.send(new RemoveInterfaces());
        }
        if (client.checkBankInterface) {
            client.checkBankInterface = false;
            client.checkItemUpdate();
            client.send(new RemoveInterfaces());
        }
        if (client.isPartyInterface) {
            client.isPartyInterface = false;
            client.checkItemUpdate();
            client.send(new RemoveInterfaces());
        }
        if (client.inDuel && !client.duelFight) {
            Client other = client.getClient(client.duel_with);
            if (other == null || !client.validClient(client.duel_with) || System.currentTimeMillis() - client.lastButton < 600) {
                return;
            }
            client.declineDuel();
            client.checkItemUpdate();
        }
        if (client.inTrade) {
            Client other = client.getClient(client.trade_reqId);
            if (other == null || !client.validClient(client.trade_reqId) || System.currentTimeMillis() - client.lastButton < 600) {
                return;
            }
            client.declineTrade();
            client.checkItemUpdate();
        }
        if (client.currentSkill >= 0) client.currentSkill = -1; // Close skill menu interface
    }
}
