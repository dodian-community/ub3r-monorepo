package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

/**
 * Sets gold crafting items in an interface slot.
 * This replaces the legacy setGoldItems() method with proper Netty implementation.
 * 
 * Packet structure (must match Mystic's SEND_UPDATE_ITEMS handler):
 * - Opcode: 53 (variable size word)
 * - Interface ID: 4 bytes (int)
 * - Item count: 2 bytes
 * - For each item:
 *   - Amount: 4 bytes (always 1)
 *   - Item ID: 2 bytes (item ID + 1)
 */
public class SetGoldItems implements OutgoingPacket {

    private final int slot;
    private final int[] items;

    /**
     * Creates a new SetGoldItems packet.
     * 
     * @param slot The interface slot ID
     * @param items Array of item IDs to display
     */
    public SetGoldItems(int slot, int[] items) {
        this.slot = slot;
        this.items = items;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);

        message.putInt(slot);
        message.putShort(items.length);

        StringBuilder preview = new StringBuilder();
        for (int item : items) {
            message.putInt(1);
            message.putShort(item + 1);
            if (preview.length() < 120) {
                if (preview.length() > 0) {
                    preview.append(", ");
                }
                preview.append(item).append("x1");
            }
        }

        ItemContainerTrace.log(client, "SetGoldItems", slot, items.length, preview.toString());
        client.send(message);
    }
}
