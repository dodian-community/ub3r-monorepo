package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent to update the client's bank interface with a specific set of items.
 */
public class SendBankItems implements OutgoingPacket {

    private final List<Integer> itemIds;
    private final List<Integer> amounts;
    private final int interfaceId;

    /**
     * Creates a new SendBankItems packet with the specified item IDs and amounts.
     * 
     * @param itemIds List of item IDs to send
     * @param amounts List of corresponding item amounts
     * @param interfaceId The interface ID to update (default is 5382 for bank)
     */
    public SendBankItems(List<Integer> itemIds, List<Integer> amounts, int interfaceId) {
        this.itemIds = new ArrayList<>(itemIds);
        this.amounts = new ArrayList<>(amounts);
        this.interfaceId = interfaceId;
        System.out.println("SendBankItems: for npc " + itemIds + ", " + amounts + ", " + interfaceId);
    }

    /**
     * Creates a new SendBankItems packet with the default bank interface ID (5382).
     * 
     * @param itemIds List of item IDs to send
     * @param amounts List of corresponding item amounts
     */
    public SendBankItems(List<Integer> itemIds, List<Integer> amounts) {
        this(itemIds, amounts, 5382);
    }

    @Override
    public void send(Client client) {
        if (itemIds.size() != amounts.size()) {
            throw new IllegalArgumentException("Item IDs and amounts lists must be of equal size");
        }

        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        message.putShort(interfaceId); // writeWord - interface ID
        message.putShort(itemIds.size()); // writeWord - number of items

        for (int i = 0; i < itemIds.size(); i++) {
            int itemId = itemIds.get(i);
            int amount = amounts.get(i);
            
            // Handle large quantities
            if (amount > 254) {
                message.put(255); // writeByte - flag for large amount
                // writeDWord_v2 - scrambled byte order [16-23][24-31][0-7][8-15]
                message.put((amount >> 16) & 0xFF); // bits 16-23
                message.put((amount >> 24) & 0xFF); // bits 24-31
                message.put(amount & 0xFF);         // bits 0-7
                message.put((amount >> 8) & 0xFF);  // bits 8-15
            } else {
                message.put(amount); // writeByte - small amount
            }
            
            // writeWordBigEndianA: little-endian with ADD transformation on first byte
            // Note: Original code adds 1 to item ID, preserving that behavior
            message.putShort(itemId + 1, ByteOrder.LITTLE, ValueType.ADD);
        }
        
        client.send(message);
    }
}
