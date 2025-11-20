package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.*;

public class SetSmithing implements OutgoingPacket {

    private int writeFrame;

    public SetSmithing(int writeFrame) {
        this.writeFrame = writeFrame;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);

        // Write interface ID as int (4 bytes) - matches client's incoming.readInt()
        message.putInt(writeFrame);

        // Write item count as short (2 bytes) - matches client's incoming.readShort()
        message.putShort(Constants.SmithingItems.length);

        for (int i = 0; i < Constants.SmithingItems.length; i++) {
            int itemId = Constants.SmithingItems[i][0] + 1;
            int amount = Constants.SmithingItems[i][1];

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
