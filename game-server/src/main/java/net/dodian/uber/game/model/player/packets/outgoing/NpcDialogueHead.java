package net.dodian.uber.game.model.player.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;

public class NpcDialogueHead implements OutgoingPacket {

    private int mainFrame, subFrame;

    public NpcDialogueHead(int mainFrame, int subFrame) {
        this.mainFrame = mainFrame;
        this.subFrame = subFrame;
    }

    @Override
    public void send(Client client) {
        client.getOutputStream().createFrame(75);
        client.getOutputStream().writeWordBigEndianA(mainFrame);
        client.getOutputStream().writeWordBigEndianA(subFrame);
        client.flushOutStream();
    }

}
