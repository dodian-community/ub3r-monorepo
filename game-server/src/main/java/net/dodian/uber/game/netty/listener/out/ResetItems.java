package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;

public class ResetItems implements OutgoingPacket {

    private final int writeFrame;

    public ResetItems(int writeFrame) {
        this.writeFrame = writeFrame;
    }

    @Override
    public void send(Client client) {

        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        message.putInt(writeFrame);
        message.putShort(client.playerItems.length);
        StringBuilder preview = new StringBuilder();
        for (int i = 0; i < client.playerItems.length; i++) {
            int amount = client.playerItemsN[i];
            message.putInt(amount);
            if (amount != 0) {
                message.putShort(client.playerItems[i]);
            }
            if (preview.length() < 120) {
                if (preview.length() > 0) {
                    preview.append(", ");
                }
                preview.append(client.playerItems[i] - 1).append('x').append(amount);
            }
        }
        ItemContainerTrace.log(client, "ResetItems", writeFrame, client.playerItems.length, preview.toString());
        client.send(message);
    }
}
