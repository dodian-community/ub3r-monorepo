package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ValueType;

public class SendSideTab implements OutgoingPacket {

    private int tabId;

    public SendSideTab(int tabId) {
        this.tabId = tabId;
    }

    @Override
    public void send(Client client) {
        System.out.println("SendSideTab: " + tabId);
        ByteMessage message = ByteMessage.message(106);
        message.put(tabId, ValueType.NEGATE);
        client.send(message);
    }

}
