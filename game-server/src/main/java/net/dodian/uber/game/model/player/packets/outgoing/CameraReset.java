package net.dodian.uber.game.model.player.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;

public class CameraReset implements OutgoingPacket {

    @Override
    public void send(Client client) {
        client.getOutputStream().createFrame(107);
    }

}
