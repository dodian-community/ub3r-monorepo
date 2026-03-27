package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sends a frame 164 packet to display an interface.
 * This replaces the legacy sendChatboxInterface() method with proper Netty implementation.
 * <p>
 * Packet structure:
 * - Opcode: 164 (variable short - interface display packet)
 * - Frame: 2 bytes (interface ID to display)
 */
public record SendFrame164(int frame) implements OutgoingPacket {
    /**
     * Creates a new SendFrame164 packet.
     *
     * @param frame The frame parameter
     */
    public SendFrame164 {
    }

    @Override
    public void send(Client client) {
        // Client's method434() reads exactly 2 bytes in little-endian format
        ByteMessage message = ByteMessage.message(164, MessageType.FIXED);

        // Send interface ID as little-endian short (matching method434())
        message.putShort(frame, ByteOrder.LITTLE);

        client.send(message);
    }
}
