package net.dodian.uber.game.model.player.packets.outgoing;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;

public class PlayerDialogueHead implements OutgoingPacket {

    private int mainFrame;

    public PlayerDialogueHead(int mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public void send(Client client) {
        client.getOutputStream().createFrame(185);
        client.getOutputStream().writeWordBigEndianA(mainFrame);
        //client.flushOutStream();
    }

}
