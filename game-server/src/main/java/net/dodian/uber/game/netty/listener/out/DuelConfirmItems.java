package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

import java.util.Collection;

/**
 * Updates duel confirmation screen with items.
 * This replaces the legacy confirmDuel() item sending logic with proper Netty implementation.
 * 
 * The interface ID changes based on the other player's item count:
 * - For own items: 6509 if other has >= 14 items, otherwise 6507
 * - For other's items: 6508 if other has >= 14 items, otherwise 6502
 * 
 * Packet structure (must match Mystic's SEND_UPDATE_ITEMS handler):
 * - Opcode: 53 (variable size word)
 * - Interface ID: 4 bytes (int)
 * - Item count: 2 bytes (short)
 * - For each item:
 *   - Amount: 4 bytes (int)
 *   - Item ID: 2 bytes (short) - only if amount > 0
 */
public class DuelConfirmItems implements OutgoingPacket {

    private final Collection<GameItem> ownItems;
    private final Collection<GameItem> otherItems;
    private final boolean isForOwnItems;

    /**
     * Creates a new DuelConfirmItems packet for own items.
     * 
     * @param ownItems The player's offered items
     * @param otherItems The other player's offered items (used to determine interface ID)
     */
    public static DuelConfirmItems forOwnItems(Collection<GameItem> ownItems, Collection<GameItem> otherItems) {
        return new DuelConfirmItems(ownItems, otherItems, true);
    }

    /**
     * Creates a new DuelConfirmItems packet for other player's items.
     * 
     * @param ownItems The player's offered items (used to determine interface ID)
     * @param otherItems The other player's offered items
     */
    public static DuelConfirmItems forOtherItems(Collection<GameItem> ownItems, Collection<GameItem> otherItems) {
        return new DuelConfirmItems(ownItems, otherItems, false);
    }

    private DuelConfirmItems(Collection<GameItem> ownItems, Collection<GameItem> otherItems, boolean isForOwnItems) {
        this.ownItems = ownItems;
        this.otherItems = otherItems;
        this.isForOwnItems = isForOwnItems;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);

        int interfaceId;
        Collection<GameItem> itemsToSend;

        if (isForOwnItems) {
            interfaceId = otherItems.size() >= 14 ? 6509 : 6507;
            itemsToSend = ownItems;
        } else {
            interfaceId = otherItems.size() >= 14 ? 6508 : 6502;
            itemsToSend = otherItems;
        }

        message.putInt(interfaceId);
        message.putShort(itemsToSend.size());

        StringBuilder preview = new StringBuilder();
        for (GameItem item : itemsToSend) {
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

        ItemContainerTrace.log(client, "DuelConfirmItems", interfaceId, itemsToSend.size(), preview.toString());
        client.send(message);
    }
}
