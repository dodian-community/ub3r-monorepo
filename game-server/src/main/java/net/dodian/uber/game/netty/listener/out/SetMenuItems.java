package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.*;

/**
 * Sends the shop/menu item list to interface 8847.
 * Legacy server wrote:
 *   createFrameVarSizeWord(53)
 *   writeWord(8847)
 *   writeWord(size)
 *   foreach item -> writeByte(1); writeWordBigEndianA(id+1)
 */
public class SetMenuItems implements OutgoingPacket {

    private final int[] items;

    public SetMenuItems(int[] items) {
        this.items = items;
    }

    @Override
    public void send(Client client) {
        System.out.println("Set menu items: " + items.length);
        ByteMessage msg = ByteMessage.message(53, MessageType.VAR_SHORT);
        msg.putShort(8847);                       // interface id
        msg.putShort(items.length);               // number of items
        for (int id : items) {
            msg.put(1);                           // amount 1
            msg.putShort(id + 1, ByteOrder.LITTLE, ValueType.ADD); // item id
        }
        client.send(msg);
    }
}
