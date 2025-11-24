package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.*;

import java.util.List;

/**
 * Sends an inventory (or any item container) update to the client.
 * <p>
 * Opcode: 53 (variable-short length)
 * Layout matching Client.java:15522-15582:
 *   - interface id:        int (4 bytes, big-endian)
 *   - item count:          short (2 bytes, big-endian)
 *   - for each item:
 *        * amount:         int (4 bytes, big-endian)
 *        * item id:        short (2 bytes, big-endian) - only if amount != 0
 */
public class SendInventory implements OutgoingPacket {

    private final int interfaceId;
    private final List<GameItem> items;

    public SendInventory(int interfaceId, List<GameItem> items) {
        this.interfaceId = interfaceId;
        this.items = items;
    }

    @Override
    public void send(Client client) {
        System.out.println("Send inventory: " + interfaceId);
        ByteMessage msg = ByteMessage.message(53, MessageType.VAR_SHORT);
        // interface id as int to match client.readInt()
        msg.putInt(interfaceId);
        // number of items as short
        msg.putShort(items.size(), ByteOrder.BIG);

        for (GameItem item : items) {
            int amount = item.getAmount();
            // Client ALWAYS reads amount as int (4 bytes)
            msg.putInt(amount, ByteOrder.BIG);

            // Client only reads item ID if amount != 0
            if (amount != 0) {
                msg.putShort(item.getId(), ByteOrder.BIG);
            }
        }
        client.send(msg);
    }
}
