package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

import java.util.Collection;

/**
 * Updates trade interface with items.
 * This replaces the legacy resetTItems() and resetOTItems() methods with proper Netty implementation.
 *
 * Packet structure (must match mystic client's SEND_UPDATE_ITEMS handler):
 * - Opcode: 53 (variable size word)
 * - Interface ID: 4 bytes (int)
 * - Item count: 2 bytes (short)
 * - For each item:
 *   - Amount: 4 bytes (int)
 *   - Item ID: 2 bytes (short) - only if amount > 0
 */
public class TradeItemsUpdate implements OutgoingPacket {

    private final int interfaceId;
    private final Collection<GameItem> items;

    /**
     * Creates a new TradeItemsUpdate packet.
     *
     * @param interfaceId The interface ID (WriteFrame parameter)
     * @param items The collection of items to display
     */
    public TradeItemsUpdate(int interfaceId, Collection<GameItem> items) {
        this.interfaceId = interfaceId;
        this.items = items;
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
                int itemId = item.getId() < 0 ? -1 : item.getId() + 1;
                message.putShort(itemId);
            }
            if (preview.length() < 120) {
                if (preview.length() > 0) {
                    preview.append(", ");
                }
                preview.append(item.getId()).append('x').append(amount);
            }
        }

        ItemContainerTrace.log(client, "TradeItemsUpdate", interfaceId, items.size(), preview.toString());
        client.send(message);
    }
}
