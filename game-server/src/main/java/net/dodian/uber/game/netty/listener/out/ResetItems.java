package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

public class ResetItems implements OutgoingPacket {

    private final int writeFrame;

    public ResetItems(int writeFrame) {
        this.writeFrame = writeFrame;
    }

    @Override
    public void send(Client client) {

        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        // Write interface ID as int (4 bytes) - matches client's incoming.readInt()
        message.putInt(writeFrame);
        // Write item count as short (2 bytes) - matches client's incoming.readShort()
        message.putShort(client.playerItems.length);
        // Write each item
        for (int i = 0; i < client.playerItems.length; i++) {
            int amount = client.playerItemsN[i];
            // Amount as int (4 bytes) - matches client's incoming.readInt()
            message.putInt(amount);

            // Item ID only if amount > 0 - matches client's conditional read
            if (amount != 0) {
                int itemId = client.playerItems[i]; // container value (id + 1)
                // Item id as big-endian short - matches client's incoming.readShort()
                message.putShort(itemId);
            }
        }
        client.send(message);
    }
}