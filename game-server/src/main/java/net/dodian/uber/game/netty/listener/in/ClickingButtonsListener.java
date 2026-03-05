package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.content.buttons.ButtonClickDispatcher;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.event.GameEventBus;
import net.dodian.uber.game.event.events.ButtonClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.dodian.utilities.DotEnvKt.getButtonTraceEnabled;

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
        int actionIndex = -1;
        if (packet.getOpcode() == 186 && payload.isReadable()) {
            actionIndex = payload.readUnsignedByte();
        }
        client.lastButtonActionIndex = actionIndex;

        if (getButtonTraceEnabled() && logger.isTraceEnabled()) {
            logger.trace("ClickButton buttonId={} size={} player={}", actionButton, packetSize, client.getPlayerName());
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

        if (GameEventBus.INSTANCE.postWithResult(new ButtonClickEvent(client, actionButton))) {
            return;
        }

        if (ButtonClickDispatcher.tryHandle(client, actionButton)) {
            return;
        }

        if (getButtonTraceEnabled() && logger.isTraceEnabled()) {
            logger.trace("Unhandled button buttonId={} player={} iface={}", client.actionButtonId, client.getPlayerName(), client.activeInterfaceId);
        }
    }
}
