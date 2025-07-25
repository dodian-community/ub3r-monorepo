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
        message.putShort(writeFrame); // writeWord
        message.putShort(client.playerItems.length); // writeWord
        
        for (int i = 0; i < client.playerItems.length; i++) {
            if (client.playerItemsN[i] > 254) {
                message.put(255); // item's stack count. if over 254, write byte 255
                // writeDWord_v2 - scrambled byte order [16-23][24-31][0-7][8-15]
                int value = client.playerItemsN[i];
                message.put((value >> 16) & 0xFF); // bits 16-23
                message.put((value >> 24) & 0xFF); // bits 24-31
                message.put(value & 0xFF);         // bits 0-7
                message.put((value >> 8) & 0xFF);  // bits 8-15
            } else {
                message.put(client.playerItemsN[i]);
            }
            
            if (client.playerItems[i] < 0) {
                client.playerItems[i] = -1;
            }
            
            // writeWordBigEndianA: little-endian with ADD transformation on first byte
            message.putShort(client.playerItems[i], ByteOrder.LITTLE, ValueType.ADD);
        }
        
        client.send(message);
       // System.out.println("ResetItems: " + writeFrame);
    }
}