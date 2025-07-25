package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

/**
 * Sends a quest-related packet with an ID parameter.
 * This replaces the legacy sendQuestSomething() method with proper Netty implementation.
 * 
 * Packet structure:
 * - Opcode: 79 (fixed size)
 * - ID: 2 bytes (writeWordBigEndian - big-endian byte order)
 * - Value: 2 bytes (writeWordA - ValueType.ADD transformation)
 */
public class SendQuestSomething implements OutgoingPacket {

    private final int id;

    /**
     * Creates a new SendQuestSomething packet.
     * 
     * @param id The quest ID parameter
     */
    public SendQuestSomething(int id) {
        this.id = id;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(79, MessageType.FIXED);
        
        // Match old behavior: writeWordBigEndian followed by writeWordA(0)
        message.putShort(id, ByteOrder.BIG);
        message.putShort(0, ValueType.NORMAL); // Changed from ValueType.ADD to match old behavior
        //System.out.println("Sending SendQuestSomething packet with ID: " + id);
        client.send(message);
    }
}