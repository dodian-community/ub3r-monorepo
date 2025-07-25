package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sent to update the duel interface with the opponent's equipment.
 * This packet updates all equipment slots in the duel interface.
 * 
 * Packet structure for each equipment slot:
 * - Opcode: 34 (variable size word)
 * - Interface ID: 2 bytes (13824 - duel interface)
 * - Slot: 1 byte (equipment slot)
 * - Item ID: 2 bytes (equipment item ID + 1, or 0 if no item)
 * - Amount: 1 byte (if amount <= 254) or 5 bytes (if amount > 254, first byte is 255 followed by 4-byte amount)
 */
public class DuelArmourUpdate implements OutgoingPacket {

    private final int[] equipment;
    private final int[] equipmentN;

    /**
     * Creates a new DuelArmourUpdate packet.
     * 
     * @param equipment The player's equipment array
     * @param equipmentN The player's equipment amounts array
     */
    public DuelArmourUpdate(int[] equipment, int[] equipmentN) {
        this.equipment = equipment;
        this.equipmentN = equipmentN;
    }

    @Override
    public void send(Client client) {
        // Create a message for each equipment slot
        for (int slot = 0; slot < equipment.length; slot++) {
            ByteMessage message = ByteMessage.message(34, MessageType.VAR_SHORT);
            
            // Write interface ID (13824 for duel interface)
            message.putShort(13824);
            
            // Write slot ID
            message.put(slot);
            
            // Write item ID (add 1 to the item ID, or 0 if no item)
            int itemId = equipment[slot];
            message.putShort(itemId < 1 ? 0 : itemId + 1);
            
            // Write amount
            int amount = equipmentN[slot];
            if (amount > 254) {
                message.put(255);
                message.putInt(amount);
            } else {
                message.put(amount);
            }
            
            // Send the packet
            client.send(message);
        }
    }
}
