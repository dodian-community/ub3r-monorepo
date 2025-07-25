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
 * Updates duel confirmation screen with items.
 * This replaces the legacy confirmDuel() item sending logic with proper Netty implementation.
 * 
 * The interface ID changes based on the other player's item count:
 * - For own items: 6509 if other has >= 14 items, otherwise 6507
 * - For other's items: 6508 if other has >= 14 items, otherwise 6502
 * 
 * Packet structure:
 * - Opcode: 53 (variable size word)
 * - Interface ID: 2 bytes (dynamic based on item count)
 * - Item count: 2 bytes
 * - For each item:
 *   - Amount: 1 byte (if <= 254) or 1 byte (255) + 4 bytes (if > 254)
 *   - Item ID: 2 bytes (writeWordBigEndianA - item ID + 1, or -1 if negative)
 */
public class DuelConfirmItems implements OutgoingPacket {

    private final Collection<GameItem> ownItems;
    private final Collection<GameItem> otherItems;
    private final boolean isForOwnItems;

    /**
     * Creates a new DuelConfirmItems packet for own items.
     * 
     * @param ownItems The player's offered items
     * @param otherItems The other player's offered items (used to determine interface ID)
     */
    public static DuelConfirmItems forOwnItems(Collection<GameItem> ownItems, Collection<GameItem> otherItems) {
        return new DuelConfirmItems(ownItems, otherItems, true);
    }

    /**
     * Creates a new DuelConfirmItems packet for other player's items.
     * 
     * @param ownItems The player's offered items (used to determine interface ID)
     * @param otherItems The other player's offered items
     */
    public static DuelConfirmItems forOtherItems(Collection<GameItem> ownItems, Collection<GameItem> otherItems) {
        return new DuelConfirmItems(ownItems, otherItems, false);
    }

    private DuelConfirmItems(Collection<GameItem> ownItems, Collection<GameItem> otherItems, boolean isForOwnItems) {
        this.ownItems = ownItems;
        this.otherItems = otherItems;
        this.isForOwnItems = isForOwnItems;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        
        // Determine interface ID and items to send
        int interfaceId;
        Collection<GameItem> itemsToSend;
        
        if (isForOwnItems) {
            // For own items: 6509 if other has >= 14 items, otherwise 6507
            interfaceId = otherItems.size() >= 14 ? 6509 : 6507;
            itemsToSend = ownItems;
        } else {
            // For other's items: 6508 if other has >= 14 items, otherwise 6502
            interfaceId = otherItems.size() >= 14 ? 6508 : 6502;
            itemsToSend = otherItems;
        }
        
        // Write interface ID
        message.putShort(interfaceId);
        
        // Write item count
        message.putShort(itemsToSend.size());
        
        // Write each item
        for (GameItem item : itemsToSend) {
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
            // Handle negative item IDs
            int itemId = item.getId() < 0 ? -1 : item.getId() + 1;
            message.putShort(itemId, ByteOrder.LITTLE, ValueType.ADD);
        }
        
        client.send(message);
    }
}