package net.dodian.uber.game.netty.listener.out;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.social.exchange.ExchangeRuntime;

public class ResetItems implements OutgoingPacket {

    private final int writeFrame;

    public ResetItems(int writeFrame) {
        this.writeFrame = writeFrame;
    }

    @Override
    public void send(Client client) {

        boolean adjusted = (writeFrame == 3214 || writeFrame == 3322) && ExchangeRuntime.session(client) != null;
        int[] itemIds = adjusted ? ExchangeRuntime.adjustedItemIds(client) : new int[client.playerItems.length];
        int[] amounts = adjusted ? ExchangeRuntime.adjustedItemAmounts(client) : client.playerItemsN;
        StringBuilder preview = new StringBuilder();
        for (int i = 0; i < client.playerItems.length; i++) {
            if (!adjusted) {
                itemIds[i] = client.playerItems[i] - 1;
            }
            int amount = amounts[i];
            if (preview.length() < 120) {
                if (preview.length() > 0) {
                    preview.append(", ");
                }
                preview.append(itemIds[i]).append('x').append(amount);
            }
        }
        ByteMessage message = TarnishItemContainerEncoder.full(writeFrame, itemIds, amounts);
        ItemContainerTrace.log(client, "ResetItems", writeFrame, client.playerItems.length, preview.toString());
        client.send(message);
    }
}
