package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.npc.NpcUpdating;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sends NPC update data to the client using proper Netty ByteMessage.
 * This replaces the direct Stream-based approach with proper packet structure.
 *
 * VERIFICATION REPORT (2025-11-19):
 * - Expected Opcode: 65 (PacketConstants.NPC_UPDATING)
 * - Actual Opcode: 65
 * - Status: VERIFIED - MATCH
 * - Client parsing: Client.java:15658-15662 calls updateNPCs(incoming, packetSize)
 * - MessageType: VAR_SHORT (matches variable size packet with word-length header)
 * - Structure verified against mystic-updatedclient expectations
 */
public class NpcUpdatePacket implements OutgoingPacket {
    
    private final Player player;
    
    public NpcUpdatePacket(Player player) {
        this.player = player;
    }
    
    @Override
    public void send(Client client) {
        // Create the message with proper opcode and type (matches original createFrameVarSizeWord(65))
        ByteMessage msg = ByteMessage.message(65, MessageType.VAR_SHORT);
        
        try {
            // Use the existing NpcUpdating logic to build the packet content
            NpcUpdating.getInstance().update(player, msg);
            
            // Send the message (this will handle encryption)
            client.send(msg);
        } catch (Exception e) {
            msg.releaseAll();
            throw e;
        }
    }
}