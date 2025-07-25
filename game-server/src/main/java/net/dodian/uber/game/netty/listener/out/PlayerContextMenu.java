package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

/**
 * Sets up player right-click context menu options.
 * This replaces the legacy process() method Stream-based menu setup with proper Netty implementation.
 * 
 * Packet structure:
 * - Opcode: 104 (variable size)
 * - Command slot: 1 byte (writeByteC - ValueType.NEGATE)
 * - Enabled: 1 byte (writeByteA - ValueType.ADD)  
 * - Command text: String
 */
public class PlayerContextMenu implements OutgoingPacket {

    private final int commandSlot;
    private final boolean enabled;
    private final String command;

    /**
     * Creates a new PlayerContextMenu packet.
     * 
     * @param commandSlot The command slot (1-5)
     * @param enabled Whether the command is enabled (1) or disabled (0)
     * @param command The command text to display
     */
    public PlayerContextMenu(int commandSlot, boolean enabled, String command) {
        this.commandSlot = commandSlot;
        this.enabled = enabled;
        this.command = command;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(104, MessageType.VAR);
        
        // Write command slot (writeByteC = negate)
        message.put(commandSlot, ValueType.NEGATE);
        
        // Write enabled flag (writeByteA = add 128)
        message.put(enabled ? 1 : 0, ValueType.ADD);
        
        // Write command text
        message.putString(command);
        
        client.send(message);
    }
}