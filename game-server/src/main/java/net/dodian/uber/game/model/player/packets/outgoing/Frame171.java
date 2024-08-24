package net.dodian.uber.game.model.player.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;

public class Frame171 implements OutgoingPacket {

    private int mainFrame, subFrame;

    public Frame171(int mainFrame, int subFrame) {
        this.mainFrame = mainFrame;
        this.subFrame = subFrame;
    }

    @Override
    public void send(Client client) {
        client.getOutputStream().createFrame(171);
        client.getOutputStream().writeByte(mainFrame);
        client.getOutputStream().writeWord(subFrame);
        //client.flushOutStream();
    }

}
