package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;

import static net.dodian.uber.ServerKt.getCommandsLibrary;

public class Commands implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        getCommandsLibrary().processCommand(client, client.getInputStream().readString());
    }
}