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
 * Sends items to virtual screen (interface 6822).
 * This replaces the legacy itemsToVScreen_old() method with proper Netty implementation.
 * 
 * Packet structure:
 * - Opcode: 53 (variable size word)
 * - Interface ID: 2 bytes (6822)
 * - Item count: 2 bytes
 * - For each item:
 *   - Amount: 1 byte (if <= 254) or 1 byte (255) + 4 bytes (if > 254)
 *   - Item ID: 2 bytes (writeWordBigEndianA - item ID + 1)
 */
public class ItemsToVScreen implements OutgoingPacket {

    private final Collection<GameItem> items;

    /**
     * Creates a new ItemsToVScreen packet.
     * 
     * @param items The collection of items to display
     */
    public ItemsToVScreen(Collection<GameItem> items) {
        this.items = items;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        
        // Write interface ID
        message.putShort(6822);
        
        // Write item count
        message.putShort(items.size());
        
        // Write each item
        for (GameItem item : items) {
            // Write amount
            if (item.getAmount() > 254) {
                message.put(255); // item's stack count. if over 254, write byte 255
                // writesDWord_v2 - scrambled byte order [16-23][24-31][0-7][8-15]
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
        }
        
        client.send(message);
    }
}