package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sends animation at a specific position.
 * This replaces the legacy animation2() method with proper Netty implementation.
 * 
 * Packet structure:
 * - Opcode: 4 (fixed size)
 * - Unknown byte: 1 byte (always 0)
 * - Animation ID: 2 bytes
 * - Unknown byte: 1 byte (always 0)
 * - Unknown word: 2 bytes (always 0)
 */
public class Animation2 implements OutgoingPacket {

    private final int animationId;

    /**
     * Creates a new Animation2 packet.
     * 
     * @param animationId The animation ID to play
     */
    public Animation2(int animationId) {
        this.animationId = animationId;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(4, MessageType.FIXED);
        
        message.put(0); // Unknown byte
        message.putShort(animationId); // Animation ID
        message.put(0); // Unknown byte
        message.putShort(0); // Unknown word
        
        client.send(message);
    }
}