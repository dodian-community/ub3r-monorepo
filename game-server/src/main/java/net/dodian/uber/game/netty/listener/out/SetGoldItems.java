package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

/**
 * Sets gold crafting items in an interface slot.
 * This replaces the legacy setGoldItems() method with proper Netty implementation.
 * 
 * Packet structure:
 * - Opcode: 53 (variable size word)
 * - Interface ID: 2 bytes (slot ID)
 * - Item count: 2 bytes
 * - For each item:
 *   - Amount: 1 byte (always 1)
 *   - Item ID: 2 bytes (writeWordBigEndianA - item ID + 1)
 */
public class SetGoldItems implements OutgoingPacket {

    private final int slot;
    private final int[] items;

    /**
     * Creates a new SetGoldItems packet.
     * 
     * @param slot The interface slot ID
     * @param items Array of item IDs to display
     */
    public SetGoldItems(int slot, int[] items) {
        this.slot = slot;
        this.items = items;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        
        // Write interface slot ID
        message.putShort(slot);
        
        // Write item count
        message.putShort(items.length);
        
        // Write each item
        for (int item : items) {
            message.put(1); // amount (always 1)
            message.putShort(item + 1, ByteOrder.LITTLE, ValueType.ADD); // writeWordBigEndianA
        }
        
        client.send(message);
    }
}