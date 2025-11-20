package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
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
        message.putInt(writeFrame); // interface id as int
        message.putShort(client.playerItems.length); // number of slots
        
        for (int i = 0; i < client.playerItems.length; i++) {
            int amount = client.playerItemsN[i];
            message.putInt(amount); // client.readInt()

            if (amount != 0) {
                int itemId = client.playerItems[i]; // container value (id + 1 or 0)
                if (itemId < 0) {
                    itemId = 0;
                }
                message.putShort(itemId, ByteOrder.BIG); // client.readShort()
            }
        }
        
        client.send(message);
       // System.out.println("ResetItems: " + writeFrame);
    }
}