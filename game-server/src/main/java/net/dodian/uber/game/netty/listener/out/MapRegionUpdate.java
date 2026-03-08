package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

public final class MapRegionUpdate implements OutgoingPacket {

    private final int mapRegionX;
    private final int mapRegionY;

    public MapRegionUpdate(int mapRegionX, int mapRegionY) {
        this.mapRegionX = mapRegionX;
        this.mapRegionY = mapRegionY;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(73, MessageType.FIXED);
        message.putShort(mapRegionX + 6, ByteOrder.BIG, ValueType.ADD);
        message.putShort(mapRegionY + 6, ByteOrder.BIG);
        client.send(message);
    }
}
