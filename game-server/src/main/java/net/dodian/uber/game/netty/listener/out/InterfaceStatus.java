package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

import net.dodian.uber.game.netty.codec.ByteOrder;

/**
 * Handles showing or hiding an interface
 * 
 * @author Dashboard
 */
public class InterfaceStatus implements OutgoingPacket {

    private final int interfaceId;
    private final boolean show;

    /**
     * Creates a new interface status packet
     * 
     * @param interfaceId the interface ID to show/hide
     * @param show whether to show (true) or hide (false) the interface
     */
    public InterfaceStatus(int interfaceId, boolean show) {
        this.interfaceId = interfaceId;
        this.show = show;
    }

    @Override
    public void send(Client client) {
        ByteMessage msg = ByteMessage.message(171);
        // Match old client's expected format: put(show ? 0 : 1) followed by putShort(interfaceId)
        msg.put(show ? 0 : 1);
        msg.putShort(interfaceId);
        System.out.println("InterfaceStatus: " + (show ? "Show" : "Hide") + " interface " + interfaceId);
        client.send(msg);
    }
}
