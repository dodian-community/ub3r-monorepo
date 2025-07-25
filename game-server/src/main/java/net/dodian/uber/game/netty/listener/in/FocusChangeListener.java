package net.dodian.uber.game.netty.listener.in;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the focus change packet (opcode 3).
 * This packet is sent when the client window gains or loses focus.
 */
@PacketHandler(opcode = 3)
public class FocusChangeListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(FocusChangeListener.class);

    static {
        // Register this handler for opcode 3
        PacketListenerManager.register(3, new FocusChangeListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        try {
            // The payload is a single byte indicating focus state:
            // 1 = window gained focus
            // 0 = window lost focus
            int focusState = packet.getPayload().readByte() & 0xFF;
            
            if (logger.isDebugEnabled()) {
                logger.debug("Client {} focus state changed to: {}", client.getPlayerName(), focusState);
            }
            
            // Update client focus state if needed
            client.setWindowFocused(focusState == 1);
            
        } catch (Exception e) {
            logger.error("Error handling focus change packet for " + client.getPlayerName(), e);
        }
    }
}
