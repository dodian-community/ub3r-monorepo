package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.engine.systems.net.PacketInteractionRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@net.dodian.uber.game.netty.listener.PacketHandler(opcodes = {128})
public class TradeRequestListener implements PacketListener {
    private static final Logger logger = LoggerFactory.getLogger(TradeRequestListener.class);

    @Override
    public void handle(Client client, GamePacket packet) {
        // Opcode 128 is retained for the legacy slot-1 action. The current menu
        // uses it for rubber-chicken "Whack"; older clients may use it for Duel.
        net.dodian.uber.game.netty.game.decode.TarnishPackets.PlayerMenuClick msg =
                net.dodian.uber.game.netty.game.decode.TarnishPackets.PlayerMenuClick.decode(packet.opcode(), packet.payload());
        if (msg == null) {
            return;
        }
        int targetSlot = msg.playerIndex();
        Client other = client.getClient(targetSlot);
        if (!client.validClient(targetSlot) || client.getSlot() == targetSlot) {
            return;
        }
        PacketInteractionRequestService.handleLegacyPrimaryPlayerAction(client, targetSlot, other);
        if (logger.isTraceEnabled()) {
            logger.trace("{} sent legacy primary player action to slot {} ({})", client.getPlayerName(), targetSlot, other.getPlayerName());
        }
    }
}
