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
        message.putShort(3900); // writeWord - interface ID
        message.putShort(ShopHandler.MaxShopItems); // writeWord - max items

        for (int i = 0; i < ShopHandler.MaxShopItems; i++) {
            if (ShopHandler.ShopItems[shopId][i] > 0) {
                int amount = ShopHandler.ShopItemsN[shopId][i];
                if (amount > 254) {
                    message.put(255); // writeByte - flag for large amount
                    // writeDWord_v2 - scrambled byte order [16-23][24-31][0-7][8-15]
                    message.put((amount >> 16) & 0xFF); // bits 16-23
                    message.put((amount >> 24) & 0xFF); // bits 24-31
                    message.put(amount & 0xFF);         // bits 0-7
                    message.put((amount >> 8) & 0xFF);  // bits 8-15
                } else {
                    message.put(amount); // writeByte - small amount
                }
                // writeWordBigEndianA: little-endian with ADD transformation on first byte
                message.putShort(ShopHandler.ShopItems[shopId][i], ByteOrder.LITTLE, ValueType.ADD);
            } else {
                message.put(0); // writeByte - zero amount
                message.putShort(0, ByteOrder.LITTLE, ValueType.ADD); // writeWordBigEndianA - zero item ID
            }
        }
        
        client.send(message);
    }
}
