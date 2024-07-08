package net.dodian.uber.game.network.packets;

import net.dodian.uber.game.model.entity.player.Client;

public interface OutgoingPacket {

    public abstract void send(Client client);

}
