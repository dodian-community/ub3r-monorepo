package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;

public class PlayerDialogueHead implements OutgoingPacket {

    private int mainFrame;

    public PlayerDialogueHead(int mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(185);
        message.putShort(mainFrame, ByteOrder.LITTLE, ValueType.ADD);
        client.send(message);
    }

}
