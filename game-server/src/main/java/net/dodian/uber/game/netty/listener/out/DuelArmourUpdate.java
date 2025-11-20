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

            // Match mystic client's UPDATE_SPECIFIC_ITEM layout:
            // interfaceId (uShort), slot (uByte), amount (int), id (uShort)

            // Duel armour interface ID
            message.putShort(13824);

            // Equipment slot index
            message.put(slot);

            // Safe amount for this slot
            int safeAmount = equipmentN != null && slot < equipmentN.length ? equipmentN[slot] : 0;
            if (safeAmount < 0) {
                safeAmount = 0;
            }
            message.putInt(safeAmount);

            // Container item id: id + 1 when there is an item and amount, otherwise 0 to clear the slot
            int itemId = equipment != null && slot < equipment.length ? equipment[slot] : -1;
            int containerId = (safeAmount > 0 && itemId > 0) ? (itemId + 1) : 0;
            message.putShort(containerId);
            System.out.println("[DUEL ARMOUR] sending to " + client.getPlayerName() + " slot=" + slot
                    + " amount=" + safeAmount + " containerId=" + containerId);

            // Send the packet
            client.send(message);
        }
    }
}
