package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.*;

public class SetSmithing implements OutgoingPacket {

    private final int writeFrame;
    private final int[][] smithingItems;

    public SetSmithing(int writeFrame) {
        this.writeFrame = writeFrame;
        this.smithingItems = Constants.SmithingItems;
    }

    public SetSmithing(int writeFrame, int[][] smithingItems) {
        this.writeFrame = writeFrame;
        this.smithingItems = smithingItems;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);

        // Write interface ID as int (4 bytes) - matches client's incoming.readInt()
        message.putInt(writeFrame);

        // Write item count as short (2 bytes) - matches client's incoming.readShort()
        message.putShort(smithingItems.length);

        for (int i = 0; i < smithingItems.length; i++) {
            int itemId = smithingItems[i][0] + 1;
            int amount = smithingItems[i][1];

            // Amount as int (4 bytes) - matches client's incoming.readInt()
            message.putInt(amount);

            // Item ID only if amount > 0 - matches client's conditional read
            if (amount != 0) {
                message.putShort(itemId);
            }
        }
        client.send(message);
    }
}
