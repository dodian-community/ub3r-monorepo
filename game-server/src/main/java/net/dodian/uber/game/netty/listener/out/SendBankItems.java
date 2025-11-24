package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent to update the client's bank interface with a specific set of items.
 */
public class SendBankItems implements OutgoingPacket {

    private final List<Integer> itemIds;
    private final List<Integer> amounts;
    private final int interfaceId;

    /**
     * Creates a new SendBankItems packet with the specified item IDs and amounts.
     * 
     * @param itemIds List of item IDs to send
     * @param amounts List of corresponding item amounts
     * @param interfaceId The interface ID to update (default is 5382 for bank)
     */
    public SendBankItems(List<Integer> itemIds, List<Integer> amounts, int interfaceId) {
        this.itemIds = new ArrayList<>(itemIds);
        this.amounts = new ArrayList<>(amounts);
        this.interfaceId = interfaceId;
        System.out.println("SendBankItems: for npc " + itemIds + ", " + amounts + ", " + interfaceId);
    }

    /**
     * Creates a new SendBankItems packet with the default bank interface ID (5382).
     * 
     * @param itemIds List of item IDs to send
     * @param amounts List of corresponding item amounts
     */
    public SendBankItems(List<Integer> itemIds, List<Integer> amounts) {
        // Default to first bank tab container (50300) for mystic client's bank tabs
        this(itemIds, amounts, 50300);
    }

    @Override
    public void send(Client client) {
        if (itemIds.size() != amounts.size()) {
            throw new IllegalArgumentException("Item IDs and amounts lists must be of equal size");
        }

        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);

        // Mystic client SEND_UPDATE_ITEMS layout:
        // int interfaceId, short itemCount,
        // then for each slot: int amount, and if amount != 0 then short id (container value)

        message.putInt(interfaceId);            // interface ID as int
        message.putShort(itemIds.size());       // number of items

        for (int i = 0; i < itemIds.size(); i++) {
            int itemId = itemIds.get(i);
            int amount = amounts.get(i);

            // Amount as full int to match incoming.readInt()
            message.putInt(amount);

            if (amount != 0) {
                int containerId = itemId + 1;  // container value (id + 1)
                message.putShort(containerId, ByteOrder.BIG);
            }
        }

        client.send(message);
    }
}
