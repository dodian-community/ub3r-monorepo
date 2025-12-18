package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.*;
import net.dodian.uber.game.party.RewardItem;

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
        ByteMessage msg = ByteMessage.message(53, MessageType.VAR_SHORT);
        msg.putInt(8847);                       // interface id
        // Write item count as short (2 bytes) - matches client's incoming.readShort()
        msg.putShort(items.length);
        // Write each item
        for (int id : items) {
            msg.putInt(1);
            // Item id as big-endian short - matches client's incoming.readShort()
            msg.putShort(id + 1);
        }
        client.send(msg);
    }
}
