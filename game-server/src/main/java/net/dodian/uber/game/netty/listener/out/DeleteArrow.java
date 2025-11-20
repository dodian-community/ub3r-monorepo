package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;

/**
 * Sent to update the client when an arrow is consumed during combat.
 * This packet updates the arrow count in the equipment interface.
 * 
 * Packet structure:
 * - Opcode: 34 (variable size word)
 * - Interface ID: 2 bytes (1688 - equipment interface)
 * - Slot: 1 byte (arrow slot)
 * - Item ID: 2 bytes (current arrow item ID + 1, or 0 if no arrows left)
 * - Amount: 1 byte (if amount <= 254) or 5 bytes (if amount > 254, first byte is 255 followed by 4-byte amount)
 */
public class DeleteArrow implements OutgoingPacket {

    private final int itemId;
    private final int slot;
    private final int amount;

    /**
     * Creates a new DeleteArrow packet.
     * 
     * @param itemId The ID of the arrow item (or -1 if no arrows left)
     * @param slot The equipment slot for arrows
     * @param amount The remaining number of arrows
     */
    public DeleteArrow(int itemId, int slot, int amount) {
        this.itemId = itemId;
        this.slot = slot;
        this.amount = amount;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(34, MessageType.VAR_SHORT);

        // Match mystic client's UPDATE_SPECIFIC_ITEM layout:
        // interfaceId (uShort), slot (uByte), amount (int), id (uShort)

        // Equipment interface ID
        message.putShort(1688);

        // Equipment slot
        message.put(slot);

        // Remaining arrow amount
        int safeAmount = Math.max(0, amount);
        message.putInt(safeAmount);

        // Container item id: id + 1 when there are arrows left, otherwise 0 to clear the slot
        int containerId = (safeAmount > 0 && itemId > 0) ? (itemId + 1) : 0;
        message.putShort(containerId);

        client.send(message);
        System.out.println("Sending DeleteArrow packet for slot " + slot + " with item ID " + itemId + " and amount " + amount);
    }
}
