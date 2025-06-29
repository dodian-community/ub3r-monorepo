package net.dodian.uber.game.model.player.packets.outgoing;

import io.netty.channel.ChannelFutureListener;
import net.dodian.uber.game.networking.codec.ByteMessage;
import net.dodian.uber.game.networking.codec.MessageType;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;

/**
 * Sends opcode 109 (logout) to the client and closes the connection.
 * Works for both Netty (ByteBuf) and legacy NIO SocketHandler clients.
 */
public class Logout implements OutgoingPacket {

    private static final int LOGOUT_OPCODE = 109;

    @Override
    public void send(Client client) {
        // Netty path
        if (client.getChannel() != null) {
            try {
                int encoded = (LOGOUT_OPCODE + client.getOutStreamDecryption().getNextKey()) & 0xFF;
                ByteMessage msg = ByteMessage.message(encoded, MessageType.FIXED);
                client.getChannel().writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
            } catch (Exception ex) {
                System.err.println("Logout packet send error: " + ex.getMessage());
                client.getChannel().close();
            }
            return;
        }

        // Legacy SocketHandler path will be removed once 100% netty impletation
        client.getOutputStream().createFrame(LOGOUT_OPCODE);
        client.flushOutStream();
        if (client.getSocketHandler() != null) {
            client.getSocketHandler().logout();
        }
    }
}
