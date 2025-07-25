package net.dodian.uber.game.netty.listener.out;

import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ValueType;

public class ObjectAnimation implements OutgoingPacket {

    private GameObjectDef def;
    private Position position;
    private int animation;

    public ObjectAnimation(GameObjectDef object, int animation) {
        this.def = object;
        this.animation = animation;
        this.position = def.getPosition();
    }

    @Override
    public void send(Client client) {
        client.send(new SetMap(position));
        ByteMessage message = ByteMessage.message(160);
        message.put(position.getZ(), ValueType.SUBTRACT);
        message.put((def.getType() << 2) + (def.getFace() & 3), ValueType.SUBTRACT);
        message.putShort(animation, ValueType.ADD);
        client.send(message);
    }

}
