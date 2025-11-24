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
 *
 * VERIFICATION REPORT (2025-11-19):
 * - Expected Opcode: 81 (PacketConstants.PLAYER_UPDATING)
 * - Actual Opcode: 81
 * - Status: VERIFIED - MATCH
 * - Client parsing: Client.java:14471-14476 calls updatePlayers(packetSize, incoming)
 * - MessageType: VAR_SHORT (matches variable size packet with word-length header)
 * - Structure verified against mystic-updatedclient expectations
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