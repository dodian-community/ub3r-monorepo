package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;

public class SetInterfaceWalkable implements OutgoingPacket {

    private int id;

    public SetInterfaceWalkable(int id) {
        this.id = id;
    }

    @Override
    public void send(Client client) {
        //System.out.println("Set interface walkable: " + id);
        ByteMessage message = ByteMessage.message(208, MessageType.FIXED);
        message.putShort(id, ByteOrder.LITTLE); // low byte first for method437
        client.send(message);
    }
}
