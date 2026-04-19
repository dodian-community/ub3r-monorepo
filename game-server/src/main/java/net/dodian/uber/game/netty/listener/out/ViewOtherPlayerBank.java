package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.listener.OutgoingPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent to allow staff members to view another player's bank contents.
 */
public class ViewOtherPlayerBank implements OutgoingPacket {

    private final int interfaceId;
    private final List<GameItem> bankItems;

    /**
     * Creates a new ViewOtherPlayerBank packet.
     * 
     * @param interfaceId The interface ID to display the bank in
     * @param bankItems The list of GameItems in the other player's bank
     */
    public ViewOtherPlayerBank(int interfaceId, List<GameItem> bankItems) {
        this.interfaceId = interfaceId;
        this.bankItems = new ArrayList<>(bankItems);
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);
        message.putInt(interfaceId);
        message.putShort(bankItems.size());

        StringBuilder preview = new StringBuilder();
        for (GameItem item : bankItems) {
            int amount = item.getAmount();

            message.putInt(amount);
            if (amount != 0) {
                int itemId = Math.max(item.getId(), 0) + 1;
                message.putShort(itemId);
            }
            if (preview.length() < 120) {
                if (preview.length() > 0) {
                    preview.append(", ");
                }
                preview.append(item.getId()).append('x').append(amount);
            }
        }

        ItemContainerTrace.log(client, "ViewOtherPlayerBank", interfaceId, bankItems.size(), preview.toString());
        client.send(message);
    }
}
