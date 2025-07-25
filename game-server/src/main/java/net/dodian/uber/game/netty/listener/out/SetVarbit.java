package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ByteOrder;

/**
 * Outgoing packet for updating a client varbit.
 * Mirrors the logic in Client.varbit(int id, int value) but using Netty ByteMessage.
 */
public class SetVarbit implements OutgoingPacket {

    private final int id;
    private final int value;

    public SetVarbit(int id, int value) {
        this.id = id;
        this.value = value;
    }

    @Override
    public void send(Client client) {
        // Value -1 indicates no update required (legacy safeguard)
        if (value == -1) {
            return;
        }

        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            // Use opcode 87 with a 4-byte integer payload (legacy writeDWord_v1)
            ByteMessage msg = ByteMessage.message(87, MessageType.FIXED);
            msg.putShort(id, ByteOrder.LITTLE); // low-byte first
            msg.putInt(value, ByteOrder.MIDDLE); // matches writeDWord_v1 ordering
            client.send(msg);
        } else {
            // Use opcode 36 with a single byte payload
            ByteMessage msg = ByteMessage.message(36, MessageType.FIXED);
            msg.putShort(id, ByteOrder.LITTLE); // low-byte first
            msg.put(value);                 // single byte
            client.send(msg);
        }
    }
}
