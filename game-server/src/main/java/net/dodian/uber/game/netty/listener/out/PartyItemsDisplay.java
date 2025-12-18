package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.party.RewardItem;

import java.util.List;

/**
 * Displays party items in the balloon party interface.
 * This replaces the legacy displayItems() and displayOfferItems() methods with proper Netty implementation.
 * 
 * Packet structure:
 * - Opcode: 53 (variable size word)
 * - Interface ID: 2 bytes (2273 for party items, 2274 for offered items)
 * - Item count: 2 bytes
 * - For each item:
 *   - Amount: 1 byte (if <= 254) or 1 byte (255) + 4 bytes (if > 254)
 *   - Item ID: 2 bytes (writeWordBigEndianA - item ID + 1)
 */
public class PartyItemsDisplay implements OutgoingPacket {

    private final int interfaceId;
    private final List<RewardItem> items;

    /**
     * Creates a new PartyItemsDisplay packet.
     * 
     * @param interfaceId The interface ID (2273 for party items, 2274 for offered items)
     * @param items The list of RewardItems to display
     */
    public PartyItemsDisplay(int interfaceId, List<RewardItem> items) {
        this.interfaceId = interfaceId;
        this.items = items;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        // Write interface ID as int (4 bytes) - matches client's incoming.readInt()
        message.putInt(interfaceId);
        // Write item count as short (2 bytes) - matches client's incoming.readShort()
        message.putShort(items.size());
        // Write each item
        for (RewardItem item : items) {
            int amount = item.getAmount();
            // Amount as int (4 bytes) - matches client's incoming.readInt()
            message.putInt(amount);

            // Item ID only if amount > 0 - matches client's conditional read
            if (amount != 0) {
                int itemId = item.getId() + 1; // container value (id + 1)
                // Item id as big-endian short - matches client's incoming.readShort()
                message.putShort(itemId);
            }
        }
        client.send(message);
    }
}