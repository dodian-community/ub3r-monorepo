package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

import java.util.Collection;

/**
 * Updates duel screen with items from either player.
 * This replaces the legacy refreshDuelScreen() method with proper Netty implementation.
 *
 * Packet structure (must match mystic client's SEND_UPDATE_ITEMS handler):
 * - Opcode: 53 (variable size word)
 * - Interface ID: 4 bytes (int)
 * - Item count: 2 bytes (short)
 * - For each item:
 *   - Amount: 4 bytes (int)
 *   - Item ID: 2 bytes (short) - only if amount > 0
 */
public class DuelItemsUpdate implements OutgoingPacket {

    private final int interfaceId;
    private final Collection<GameItem> items;
    private final boolean fillEmptySlots;

    /**
     * Creates a new DuelItemsUpdate packet.
     *
     * @param interfaceId The interface ID (6669 for own items, 6670 for other's items)
     * @param items The collection of items to display
     * @param fillEmptySlots Whether to fill empty slots up to 28 slots
     */
    public DuelItemsUpdate(int interfaceId, Collection<GameItem> items, boolean fillEmptySlots) {
        this.interfaceId = interfaceId;
        this.items = items;
        this.fillEmptySlots = fillEmptySlots;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        message.putInt(interfaceId);
        message.putShort(items.size());

        StringBuilder preview = new StringBuilder();
        for (GameItem item : items) {
            int amount = item.getAmount();
            message.putInt(amount);
            if (amount != 0) {
                message.putShort(item.getId() + 1);
            }
            if (preview.length() < 120) {
                if (preview.length() > 0) {
                    preview.append(", ");
                }
                preview.append(item.getId()).append('x').append(amount);
            }
        }

        ItemContainerTrace.log(client, "DuelItemsUpdate", interfaceId, items.size(), preview.toString());
        client.send(message);
    }
}
