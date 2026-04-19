package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ValueType;

public record InventoryInterface(int interfaceId, int inventoryId) implements OutgoingPacket {

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(248);
        message.putShort(interfaceId, ValueType.ADD);
        message.putShort(inventoryId);
        client.send(message);
    }

}
