package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sends a frame 27 packet with no payload.
 * This replaces the legacy createFrame(27) calls with proper Netty implementation.
 * 
 * Packet structure:
 * - Opcode: 27 (fixed size, no payload)
 */
public class SendFrame27 implements OutgoingPacket {

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(27, MessageType.FIXED);
        client.send(message);
    }
}