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
        message.putShort(writeFrame);
        message.putShort(Constants.SmithingItems.length);
        for (int i = 0; i < Constants.SmithingItems.length; i++) {
            int itemId = Constants.SmithingItems[i][0] + 1;
            int amount = Constants.SmithingItems[i][1];
            System.out.println("Sending smithing item: " + itemId + " amount: " + amount);
            if (amount > 254) {
                message.put(255);
                message.putInt(amount, ByteOrder.LITTLE);
            } else {
                message.put(amount);
            }
            if (itemId < 0) { // Assuming this check is still needed.
                client.playerItems[i] = 7500;
            }
            message.putShort(itemId, ByteOrder.LITTLE, ValueType.ADD);
        }
        client.send(message);
    }
}
