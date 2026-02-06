package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ValueType;

public class InventoryInterface implements OutgoingPacket {

    private int interfaceId, inventoryId;

    public InventoryInterface(int interfaceId, int inventoryId) {
        this.interfaceId = interfaceId;
        this.inventoryId = inventoryId;
    }

    public int getInterfaceId() {
        return interfaceId;
    }

    public int getInventoryId() {
        return inventoryId;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(248);
        message.putShort(interfaceId, ValueType.ADD);
        message.putShort(inventoryId);
        client.send(message);
    }

}
