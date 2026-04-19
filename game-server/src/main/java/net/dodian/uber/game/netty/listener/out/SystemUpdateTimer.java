package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

public final class SystemUpdateTimer implements OutgoingPacket {

    private final int clientTicks;

    public SystemUpdateTimer(int clientTicks) {
        this.clientTicks = clientTicks;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(114, MessageType.FIXED);
        message.putShort(clientTicks, ByteOrder.LITTLE);
        client.send(message);
    }
}
