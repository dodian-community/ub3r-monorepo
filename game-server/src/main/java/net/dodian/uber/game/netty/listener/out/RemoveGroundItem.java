package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ValueType;

public class RemoveGroundItem implements OutgoingPacket {

    private GameItem item;
    private Position position;

    public RemoveGroundItem(GameItem item, Position position) {
        this.item = item;
        this.position = position;
    }

    @Override
    public void send(Client client) {
        client.send(new SetMap(position));
        ByteMessage message = ByteMessage.message(156);
        message.put(position.getZ(), ValueType.SUBTRACT); //Cant enter height due to client bug!
        message.putShort(item.getId());
        client.send(message);
    }

}
