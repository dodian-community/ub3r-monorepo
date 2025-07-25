package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

/**
 * Sent to update the client's bank interface with the current bank contents.
 */
public class ResetBank implements OutgoingPacket {

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        message.putShort(5382); // writeWord - interface ID
        message.putShort(client.bankSize()); // writeWord - number of bank items

        for (int i = 0; i < client.bankSize(); i++) {
            int amount = client.bankItemsN[i];
            
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
            
            // Handle item ID with validation
            int itemId = client.bankItems[i];
            if (amount < 1) {
                itemId = 0; // No item if amount is 0
            }
            if (itemId < 0) {
                itemId = 7500; // Default item ID if invalid
            }
            
            // writeWordBigEndianA: little-endian with ADD transformation on first byte
            message.putShort(itemId, ByteOrder.LITTLE, ValueType.ADD);
        }
        
        client.send(message);
    }
}
