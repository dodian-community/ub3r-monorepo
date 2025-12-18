package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.party.RewardItem;

/**
 * Sent to update the menu items in an interface.
 * This is similar to ShowMenuItems but with a different packet structure.
 */
public class ShowMenuItems2 implements OutgoingPacket {

    private final int[] items;
    private final int[] amounts;

    /**
     * Creates a new ShowMenuItems2 packet.
     * 
     * @param items The array of item IDs to display
     * @param amounts The array of item amounts (must be same length as items array)
     */
    public ShowMenuItems2(int[] items, int[] amounts) {
        if (items.length != amounts.length) {
            throw new IllegalArgumentException("Items and amounts arrays must be the same length");
        }
        this.items = items.clone();
        this.amounts = amounts.clone();
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        message.putInt(8847); // Interface ID
        message.putShort(items.length); // Number of items
        // Write each item
        for (int i = 0; i < items.length; i++) {
            message.putInt(amounts[i]);
            if (amounts[i] != 0)
                message.putShort(items[i] + 1);
        }
        
        client.send(message);
    }
}
