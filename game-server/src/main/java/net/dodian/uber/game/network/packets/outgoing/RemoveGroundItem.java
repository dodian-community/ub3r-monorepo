package net.dodian.uber.game.network.packets.outgoing;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.network.packets.OutgoingPacket;

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
        client.getOutputStream().createFrame(156);
        client.getOutputStream().writeByteS(position.getZ()); //Cant enter height due to client bug!
        client.getOutputStream().writeWord(item.getId());
    }

}
