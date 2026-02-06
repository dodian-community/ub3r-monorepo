package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.content.buttons.ButtonClickDispatcher;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Emotes;
import net.dodian.uber.game.model.player.quests.QuestSend;
import net.dodian.uber.game.model.player.skills.prayer.Prayers;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces;
import net.dodian.uber.game.netty.listener.out.SendFrame27;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.dodian.utilities.DotEnvKt.getServerDebugMode;

@PacketHandler(opcode = 185)
public class ClickingButtonsListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ClickingButtonsListener.class);

    static {
        PacketListenerManager.register(185, new ClickingButtonsListener());
        PacketListenerManager.register(186, new ClickingButtonsListener());
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        int packetSize = packet.getSize();
        if (packetSize < 4) {
            logger.warn("ClickingButtons opcode 185 with unexpected size {} from {}", packetSize, client.getPlayerName());
            return;
        }

        ByteBuf payload = packet.getPayload();
        int actionButton = payload.readInt();

        if (getServerDebugMode() || client.playerRights == 2) {
            client.println_debug("ClickButton: " + actionButton + " (size=" + packetSize + ")");
        }
        if (System.currentTimeMillis() - client.lastButton < 600 || !client.validClient) {
            client.lastButton = System.currentTimeMillis();
            return;
        }
        if (!(actionButton >= 9157 && actionButton <= 9194)) {
            client.actionButtonId = actionButton;
        }
        if (actionButton != 10239 && actionButton != 10238 && actionButton != 6212 && actionButton != 6211) {
            client.resetAction(false);
        }
        if (client.duelButton(actionButton)) {
            return;
        }
        Prayers.Prayer prayer = Prayers.Prayer.forButton(actionButton);
        if (prayer != null) {
            client.getPrayerManager().togglePrayer(prayer);
            return;
        }
        if (QuestSend.questMenu(client, actionButton)) {
            return;
        }
        if (client.refundSlot != -1) {
            int size = client.rewardList.size();
            int checkSlot = 1;
            int position = size - client.refundSlot;
            if (actionButton == 9158 || actionButton == 9168 || actionButton == 9179 || actionButton == 9191) {
                checkSlot = 2;
            } else if (actionButton == 9169 || actionButton == 9180 || actionButton == 9192) {
                checkSlot = 3;
            } else if (actionButton == 9181 || actionButton == 9193) {
                checkSlot = 4;
            } else if (actionButton == 9194) {
                checkSlot = 5;
            }
            if (client.refundSlot == 0 && ((size > 3 && checkSlot == 5) || (size == 3 && checkSlot == 4) || (size == 1 && checkSlot == 2) || (size == 2 && checkSlot == 3))) {
                client.refundSlot = -1;
                client.send(new RemoveInterfaces());
            } else if ((position > 3) && checkSlot == 4) {
                client.refundSlot += 3;
            } else if (client.refundSlot != 0 && ((position <= 3 && checkSlot == position + 1) || (position > 3 && checkSlot == 5))) {
                client.refundSlot -= 3;
            } else {
                client.reclaim(checkSlot);
            }
            if (!client.rewardList.isEmpty()) {
                client.setRefundOptions();
            }
            return;
        }
        if (client.herbMaking != -1) {
            int size = client.herbOptions.size();
            int checkSlot = 1;
            int position = size - client.herbMaking;
            if (actionButton == 9158 || actionButton == 9168 || actionButton == 9179 || actionButton == 9191) {
                checkSlot = 2;
            } else if (actionButton == 9169 || actionButton == 9180 || actionButton == 9192) {
                checkSlot = 3;
            } else if (actionButton == 9181 || actionButton == 9193) {
                checkSlot = 4;
            } else if (actionButton == 9194) {
                checkSlot = 5;
            }
            if (client.herbMaking == 0 && ((size > 3 && checkSlot == 5) || (size == 3 && checkSlot == 4) || (size == 1 && checkSlot == 2) || (size == 2 && checkSlot == 3))) {
                client.herbMaking = -1;
                client.send(new RemoveInterfaces());
            } else if ((position > 3) && checkSlot == 4) {
                client.herbMaking += 3;
            } else if (client.refundSlot != 0 && ((position <= 3 && checkSlot == position + 1) || (position > 3 && checkSlot == 5))) {
                client.herbMaking -= 3;
            } else if (client.herbMaking + checkSlot <= size) {
                client.send(new RemoveInterfaces());
                client.XinterfaceID = 4753;
                client.XremoveSlot = client.herbMaking + checkSlot;
                client.herbMaking = -1;
                client.send(new SendFrame27());
            }
            client.setHerbOptions();
            return;
        }

        Emotes.doEmote(actionButton, client);
        if (ButtonClickDispatcher.tryHandle(client, actionButton)) {
            return;
        }

        if (client.playerRights > 1) {
            client.println_debug("Case 185: Action Button: " + client.actionButtonId);
        }
    }
}
