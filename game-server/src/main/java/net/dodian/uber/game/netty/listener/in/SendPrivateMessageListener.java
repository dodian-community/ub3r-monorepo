package net.dodian.uber.game.netty.listener.in;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;

/**
 * Netty implementation of private-message send (opcode 126).
 */
@PacketHandler(opcode = 126)
public class SendPrivateMessageListener implements PacketListener {

    static {
        PacketListenerManager.register(126, new SendPrivateMessageListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteMessage msg = ByteMessage.wrap(packet.getPayload());
        long friend = msg.getLong();
        int remaining = msg.getBuffer().readableBytes();
        byte[] text = msg.getBytes(remaining);
        client.sendPmMessage(friend, text, remaining);
        // no further action; Client handles chat state internally
    }
}
