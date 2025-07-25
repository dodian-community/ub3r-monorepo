package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.*;

public class CreateGroundItem implements OutgoingPacket {

    private GameItem item;
    private Position position;

    public CreateGroundItem(GameItem item, Position position) {
        this.item = item;
        this.position = position;
    }

    @Override
    public void send(Client client) {
       // System.out.println("CreateGroundItem: " + item.getId() + ", " + position.getX() + ", " + position.getY() + ", " + position.getZ());
        client.send(new SetMap(position));
        ByteMessage message = ByteMessage.message(44);
        message.putShort(item.getId(), ByteOrder.LITTLE, ValueType.ADD);
        message.putShort(item.getAmount());
        message.put(position.getZ());
        client.send(message);
    }

}
