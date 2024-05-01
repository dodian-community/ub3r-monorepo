package net.dodian.uber.game.model.player.packets.outgoing;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;

public class SetMap implements OutgoingPacket {
    private final Position pos;

    public SetMap(Position pos) {
        this.pos = pos;
    }

    @Override
    public void send(Client client) {
        client.getOutputStream().createFrame(85);
        client.getOutputStream().writeByteC(pos.getY() - (client.mapRegionY * 8));
        client.getOutputStream().writeByteC(pos.getX() - (client.mapRegionX * 8));
    }
}
