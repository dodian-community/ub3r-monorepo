package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent to allow staff members to view another player's bank contents.
 */
public class ViewOtherPlayerBank implements OutgoingPacket {

    private final int interfaceId;
    private final List<GameItem> bankItems;

    /**
     * Creates a new ViewOtherPlayerBank packet.
     * 
     * @param interfaceId The interface ID to display the bank in
     * @param bankItems The list of GameItems in the other player's bank
     */
    public ViewOtherPlayerBank(int interfaceId, List<GameItem> bankItems) {
        this.interfaceId = interfaceId;
        this.bankItems = new ArrayList<>(bankItems);
        System.out.println("ViewOtherPlayerBank: Showing " + bankItems.size() + " items from another player's bank");
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        message.putShort(interfaceId); // writeWord - interface ID
        message.putShort(bankItems.size()); // writeWord - number of items
        
        System.out.println("Sending other player's bank with " + bankItems.size() + " items");
        
        for (GameItem item : bankItems) {
            int amount = item.getAmount();
            
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
            int itemId = item.getId();
            if (itemId < 0) {
                itemId = 0; // Ensure valid item ID
            }
            message.putShort(itemId + 1, ByteOrder.LITTLE, ValueType.ADD);
        }
        
        client.send(message);
    }
}
