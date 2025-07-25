package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;

/**
 * Sends opcode 109 (logout) to the client.
 * Based on Luna server's implementation.
 */
public class Logout implements OutgoingPacket {

    private static final int LOGOUT_OPCODE = 109;

    @Override
    public void send(Client client) {
        // Create a simple ByteMessage with just the logout opcode
        // Following Luna server's approach - no payload needed
        ByteMessage bm = ByteMessage.message(LOGOUT_OPCODE);
        
        if (client.getChannel() != null && client.getChannel().isActive()) {
            // Just send the packet without closing the connection
            client.getChannel().writeAndFlush(bm);
        } else {
            bm.release();
        }
    }
}
