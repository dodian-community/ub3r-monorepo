package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sent to load a private message in the client.
 * This packet is used to display a private message from another player.
 */
public class LoadPrivateMessage implements OutgoingPacket {

    private final long name;
    private final int world;

    /**
     * Creates a new LoadPrivateMessage packet.
     * 
     * @param name The encoded name of the message sender
     * @param world The world ID where the sender is located (0 = offline, >0 = world ID + 9)
     */
    public LoadPrivateMessage(long name, int world) {
        this.name = name;
        // Add 9 to world ID if not 0 (0 means offline)
        this.world = world != 0 ? world + 9 : 1;
        //System.out.println("loadpm " + name + " " + this.world);
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(50, MessageType.FIXED);
        message.putLong(name);  // Sender's encoded name
        message.put(world);     // World ID (with offset if online)
        client.send(message);
    }
}
