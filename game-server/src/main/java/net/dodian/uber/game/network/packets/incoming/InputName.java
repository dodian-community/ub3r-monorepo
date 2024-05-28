package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.Packet;
import net.dodian.utilities.Utils;

public class InputName implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) { //TODO: Fix this for future content?!
        //client.getInputStream().endFrameVarSizeWord(); //Not sure what htis is!
        client.getOutputStream().createFrameVarSize(104);
        long EnteredAmount = client.getInputStream().readDWord();
        client.getInputStream().endFrameVarSizeWord();
        System.out.println("enter: " + EnteredAmount);
        String text = Utils.longToName(EnteredAmount);
        System.out.println("text: " + text);
    }

}
