package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

import java.util.Collection;

/**
 * Updates duel screen with items from either player.
 * This replaces the legacy refreshDuelScreen() method with proper Netty implementation.
 * 
 * Packet structure:
 * - Opcode: 53 (variable size word)
 * - Interface ID: 2 bytes (6669 for own items, 6670 for other player's items)
 * - Item count: 2 bytes
 * - For each item:
 *   - Amount: 1 byte (if <= 254) or 1 byte (255) + 4 bytes (if > 254)
 *   - Item ID: 2 bytes (writeWordBigEndianA - item ID + 1)
 * - Fill remaining slots with empty items if count < 28
 */
public class DuelItemsUpdate implements OutgoingPacket {

    private final int interfaceId;
    private final Collection<GameItem> items;
    private final boolean fillEmptySlots;

    /**
     * Creates a new DuelItemsUpdate packet.
     * 
     * @param interfaceId The interface ID (6669 for own items, 6670 for other's items)
     * @param items The collection of items to display
     * @param fillEmptySlots Whether to fill empty slots up to 28 slots
     */
    public DuelItemsUpdate(int interfaceId, Collection<GameItem> items, boolean fillEmptySlots) {
        this.interfaceId = interfaceId;
        this.items = items;
        this.fillEmptySlots = fillEmptySlots;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        
        // Write interface ID
        message.putShort(interfaceId);
        
        // Write item count
        message.putShort(items.size());
        
        int current = 0;
        
        // Write each item
        for (GameItem item : items) {
            // Write amount
            if (item.getAmount() > 254) {
                message.put(255); // item's stack count. if over 254, write byte 255
                // writeDWord_v2 - scrambled byte order [16-23][24-31][0-7][8-15]
                int amount = item.getAmount();
                message.put((amount >> 16) & 0xFF); // bits 16-23
                message.put((amount >> 24) & 0xFF); // bits 24-31
                message.put(amount & 0xFF);         // bits 0-7
                message.put((amount >> 8) & 0xFF);  // bits 8-15
            } else {
                message.put(item.getAmount());
            }
            
            // Write item ID (writeWordBigEndianA = little-endian + 128)
            message.putShort(item.getId() + 1, ByteOrder.LITTLE, ValueType.ADD);
            current++;
        }
        
        // Fill remaining slots with empty items if requested and count < 28
        if (fillEmptySlots && current < 28) {
            for (int i = current; i < 28; i++) {
                message.put(1); // amount
                message.putShort(-1, ByteOrder.LITTLE, ValueType.ADD); // empty item ID
            }
        }
        
        client.send(message);
    }
}