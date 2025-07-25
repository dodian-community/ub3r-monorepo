package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerUpdating;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sends player update data to the client using proper Netty ByteMessage.
 * This replaces the direct Stream-based approach with proper packet structure.
 */
public class PlayerUpdatePacket implements OutgoingPacket {
    
    private final Player player;
    
    public PlayerUpdatePacket(Player player) {
        this.player = player;
    }
    
    @Override
    public void send(Client client) {
        // Create the message with proper opcode and type (matches original createFrameVarSizeWord(81))
        ByteMessage msg = ByteMessage.message(81, MessageType.VAR_SHORT);
        
        try {
            // Use the existing PlayerUpdating logic to build the packet content
            PlayerUpdating.getInstance().update(player, msg);
            
            // Send the message (this will handle encryption)
            client.send(msg);
        } catch (Exception e) {
            msg.releaseAll();
            throw e;
        }
    }
}