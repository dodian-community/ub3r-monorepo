package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;

public class Frame171 implements OutgoingPacket {

    private int mainFrame, subFrame;

    public Frame171(int mainFrame, int subFrame) {
        this.mainFrame = mainFrame;
        this.subFrame = subFrame;
    }

    @Override
    public void send(Client client) {
        System.out.println("Frame 171: " + mainFrame + ", " + subFrame);
        ByteMessage message = ByteMessage.message(171);
        message.put(mainFrame);
        message.putShort(subFrame);
        client.send(message);
    }

}
