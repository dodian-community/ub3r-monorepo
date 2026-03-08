package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

import java.util.Collection;

/**
 * Sends items to the duel victory screen container (interface 6822).
 *
 * Mystic's item-container update path reads:
 * - interface id: int
 * - item count: short
 * - for each item:
 *   - amount: int
 *   - item id: short (only when amount != 0)
 */
public class ItemsToVScreen implements OutgoingPacket {

    private final Collection<GameItem> items;

    /**
     * Creates a new ItemsToVScreen packet.
     * 
     * @param items The collection of items to display
     */
    public ItemsToVScreen(Collection<GameItem> items) {
        this.items = items;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);

        message.putInt(6822);
        message.putShort(items.size());

        for (GameItem item : items) {
            message.putInt(item.getAmount());
            if (item.getAmount() != 0) {
                message.putShort(item.getId() + 1);
            }
        }

        client.send(message);
    }
}
