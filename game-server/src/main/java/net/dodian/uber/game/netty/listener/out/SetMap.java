package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ValueType;

public class SetMap implements OutgoingPacket {
    private final Position pos;

    public SetMap(Position pos) {
        this.pos = pos;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(85);
        message.put(pos.getY() - (client.mapRegionY * 8), ValueType.NEGATE);
        message.put(pos.getX() - (client.mapRegionX * 8), ValueType.NEGATE);
        client.send(message);
    }
}
