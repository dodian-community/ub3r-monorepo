package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;

public class CameraReset implements OutgoingPacket {

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(107);
        client.send(message);
    }

}
