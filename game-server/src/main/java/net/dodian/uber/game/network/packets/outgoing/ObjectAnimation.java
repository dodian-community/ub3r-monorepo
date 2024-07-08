package net.dodian.uber.game.network.packets.outgoing;

import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.OutgoingPacket;

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
        client.getOutputStream().createFrame(160);
        client.getOutputStream().writeByteS(position.getZ()); //Is this height?
        client.getOutputStream().writeByteS((def.getType() << 2) + (def.getFace() & 3));
        client.getOutputStream().writeWordA(animation);
    }

}
