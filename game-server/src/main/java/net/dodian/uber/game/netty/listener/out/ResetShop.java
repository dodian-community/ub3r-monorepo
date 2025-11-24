package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.ShopHandler;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ValueType;

public class ResetShop implements OutgoingPacket {

    private final int shopId;
    private static final ShopHandler shopHandler = new ShopHandler();

    public ResetShop(int shopId) {
        this.shopId = shopId;
    }

    @Override
    public void send(Client client) {
        ByteMessage message = ByteMessage.message(53, MessageType.VAR_SHORT);

        // Mystic client SEND_UPDATE_ITEMS expects:
        // int interfaceId, short itemCount, then for each slot:
        //   int amount, and if amount != 0 then short id (container value)

        // Interface ID for shop items container
        message.putInt(3900); // matches client.readInt()
        // Number of shop slots we are sending
        message.putShort(ShopHandler.MaxShopItems);

        for (int i = 0; i < ShopHandler.MaxShopItems; i++) {
            int amount = ShopHandler.ShopItemsN[shopId][i];

            // Write amount as full int to match incoming.readInt()
            message.putInt(amount);

            if (amount != 0) {
                int itemId = ShopHandler.ShopItems[shopId][i]; // already stored as container id (id + 1)
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
