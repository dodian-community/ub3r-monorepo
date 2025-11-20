package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sent to update the wilderness level display in the client.
 * This packet updates both the wilderness level and the interface text.
 */
public class SetWildernessLevel implements OutgoingPacket {

    private final int level;

    /**
     * Creates a new SetWildernessLevel packet.
     * 
     * @param level The wilderness level to display
     */
    public SetWildernessLevel(int level) {
        this.level = level;
    }

    @Override
    public void send(Client client) {
        // Update the wilderness level interface
        ByteMessage message = ByteMessage.message(208, MessageType.FIXED);
        message.putInt(197);  // Interface ID for wilderness level, matches Client.readInt()
        client.send(message);
        
        // Update the wilderness level text
        client.send(new SendString("Level: " + level, 199));
    }
}
