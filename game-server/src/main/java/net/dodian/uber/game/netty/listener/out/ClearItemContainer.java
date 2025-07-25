package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

/**
 * Clears an item container interface by sending empty slots.
 * This replaces legacy methods that manually clear item containers.
 * 
 * Packet structure:
 * - Opcode: 53 (variable size word)
 * - Interface ID: 2 bytes
 * - Item count: 2 bytes (number of slots to clear)
 * - For each slot:
 *   - Amount: 1 byte (0)
 *   - Item ID: 2 bytes (0, with writeWordBigEndianA transformation)
 */
public class ClearItemContainer implements OutgoingPacket {

    private final int interfaceId;
    private final int slotCount;

    /**
     * Creates a new ClearItemContainer packet.
     * 
     * @param interfaceId The interface ID to clear
     * @param slotCount The number of slots to clear (typically 28)
     */
    public ClearItemContainer(int interfaceId, int slotCount) {
        this.interfaceId = interfaceId;
        this.slotCount = slotCount;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        
        // Write interface ID
        message.putShort(interfaceId);
        
        // Write item count
        message.putShort(slotCount);
        
        // Clear all slots
        for (int i = 0; i < slotCount; i++) {
            message.put(0); // amount
            message.putShort(0, ByteOrder.LITTLE, ValueType.ADD); // item ID (writeWordBigEndianA with 0)
        }
        
        client.send(message);
    }
}