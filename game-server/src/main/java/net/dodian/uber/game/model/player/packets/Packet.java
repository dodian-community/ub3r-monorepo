package net.dodian.uber.game.model.player.packets;

import net.dodian.uber.game.model.entity.player.Client;

public interface Packet {

    void ProcessPacket(Client client, int packetType, int packetSize);

}
