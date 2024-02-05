package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

public class Commands implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        String playerCommand = client.getInputStream().readString();
        if (!(playerCommand.indexOf("password") > 0) && !(playerCommand.indexOf("unstuck") > 0)) {
            client.println_debug("playerCommand: " + playerCommand);
        }
        if (client.validClient) {
            customCommand(client, playerCommand);
        } else {
            client.send(new SendMessage("Command ignored, please use another client"));
        }
    }

    public void customCommand(Client client, String command) {
    }

}