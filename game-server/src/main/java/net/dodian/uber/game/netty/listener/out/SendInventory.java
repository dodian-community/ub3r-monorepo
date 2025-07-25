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
 * Layout (legacy Stream equivalent):
 *   - interface id:        writeWord (big-endian)
 *   - size:                writeWord (big-endian)
 *   - for each item
 *        * amount > 254 ? 0xFF + writeDWord_v2(amount) : writeByte(amount)
 *        * item id:        writeWordBigEndianA(id + 1)
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
        // interface id – standard big-endian word
        msg.putShort(interfaceId, ByteOrder.BIG);
        // number of items
        msg.putShort(items.size(), ByteOrder.BIG);

        for (GameItem item : items) {
            int amount = item.getAmount();
            if (amount > 254) {
                msg.put(255); // sentinel
                // amount in INVERSE_MIDDLE order (see writeDWord_v2)
                msg.putInt(amount, ByteOrder.INVERSE_MIDDLE);
            } else {
                msg.put(amount);
            }
            // item id + 1 using writeWordBigEndianA semantics: low+128, then high
            msg.putShort(item.getId() + 1, ByteOrder.LITTLE, ValueType.ADD);
        }
        client.send(msg);
    }
}
