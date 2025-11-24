package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

/**
 * Sent to update the client's bank interface with the current bank contents.
 */
public class ResetBank implements OutgoingPacket {

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);

        // Mystic client SEND_UPDATE_ITEMS layout:
        // int interfaceId, short itemCount,
        // then for each slot: int amount, and if amount != 0 then short id (container value)

        // Use first bank tab container (50300) rather than legacy 5382
        final int bankInterfaceId = 50300;
        int bankSize = client.bankSize();

        message.putInt(bankInterfaceId); // interface ID as int
        message.putShort(bankSize);      // number of bank items

        for (int i = 0; i < bankSize; i++) {
            int amount = client.bankItemsN[i];

            // Amount as full int to match incoming.readInt()
            message.putInt(amount);

            if (amount != 0) {
                int itemId = client.bankItems[i]; // already stored as container value (id + 1)
                if (itemId < 0) {
                    itemId = 0;
                }
                // Item id as big-endian short, matching incoming.readShort()
                message.putShort(itemId, ByteOrder.BIG);
            }
        }

        client.send(message);
    }
}
