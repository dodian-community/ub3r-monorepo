package net.dodian.uber.game.model.player.packets.outgoing;

import io.netty.channel.ChannelFutureListener;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;
import net.dodian.uber.game.networking.codec.ByteMessage;
import net.dodian.uber.game.networking.codec.MessageType;

/**
 * Sends opcode 109 (logout) to the client and closes the connection.
 * Works with Netty (ByteBuf).
 */
public class Logout implements OutgoingPacket {

    private static final int LOGOUT_OPCODE = 109;

    @Override
    public void send(Client client) {
        client.getOutputStream().createFrame(LOGOUT_OPCODE);
        client.flushOutStreamAndClose();
    }
}
