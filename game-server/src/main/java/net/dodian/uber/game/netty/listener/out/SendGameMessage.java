package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.game.encode.TarnishOutboundPackets;

public class SendGameMessage implements OutgoingPacket {

    private final int id;
    private final int time;
    private final String context;

    public SendGameMessage(int id, int time, String context) {
        this.id = id;
        this.time = time;
        this.context = context;
    }

    @Override
    public void send(Client client) {
        client.send(new TarnishOutboundPackets.GameMessage(id, time, context).encode());
    }
}
