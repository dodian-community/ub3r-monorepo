package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.Packet;

import static net.dodian.utilities.DotEnvKt.getServerEnv;

public class MouseClicks implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int in = client.getInputStream().readDWord();
        if(getServerEnv().equals("dev"))
            System.out.println("clickId: " + in);
    }

}
