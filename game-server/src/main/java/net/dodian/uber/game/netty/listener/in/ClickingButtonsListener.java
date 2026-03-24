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
import net.dodian.uber.game.runtime.interaction.PlayerTickThrottleService;
import net.dodian.uber.game.skills.smithing.SmithingDefinitions;
import net.dodian.uber.game.ui.buttons.ButtonClickRequest;
import net.dodian.uber.game.ui.buttons.InterfaceButtonBinding;
import net.dodian.uber.game.ui.buttons.InterfaceButtonRegistry;
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
        int packetSize = packet.size();
        if (packetSize < 4) {
            logger.warn("ClickingButtons opcode 185 with unexpected size {} from {}", packetSize, client.getPlayerName());
            return;
        }

        ByteBuf payload = packet.payload();
        int actionButton = payload.readInt();
        int actionIndex = -1;
        if (packet.opcode() == 186 && payload.isReadable()) {
            actionIndex = payload.readUnsignedByte();
        }
        client.lastButtonActionIndex = actionIndex;

        if (client.activeInterfaceId == 2400) {
            logger.warn(
                    "Smelting button click buttonId={} actionIndex={} size={} iface={} player={}",
                    actionButton,
                    actionIndex,
                    packetSize,
                    client.activeInterfaceId,
                    client.getPlayerName()
            );
        }

        if (getButtonTraceEnabled() && logger.isTraceEnabled()) {
            logger.trace("ClickButton buttonId={} size={} player={}", actionButton, packetSize, client.getPlayerName());
        }
        if (!client.validClient || !PlayerTickThrottleService.tryAcquireMs(client, PlayerTickThrottleService.BUTTON_GENERAL, 600L)) {
            return;
        }

        if (!(actionButton >= 9157 && actionButton <= 9194)) {
            client.actionButtonId = actionButton;
        }
        boolean preserveSmeltingSelection = client.activeInterfaceId == 2400 && SmithingDefinitions.isSmeltingInterfaceButton(actionButton);
        if (!preserveSmeltingSelection && actionButton != 10239 && actionButton != 10238 && actionButton != 6212 && actionButton != 6211) {
            client.resetAction(false);
        }

        InterfaceButtonBinding resolvedBinding = InterfaceButtonRegistry.INSTANCE.resolve(client, actionButton, actionIndex);
        ButtonClickRequest request = new ButtonClickRequest(
                client,
                actionButton,
                actionIndex,
                client.activeInterfaceId,
                resolvedBinding != null ? resolvedBinding.getInterfaceId() : -1,
                resolvedBinding != null ? resolvedBinding.getComponentId() : -1,
                resolvedBinding != null ? resolvedBinding.getComponentKey() : "raw:" + actionButton
        );

        if (GameEventBus.postWithResult(new ButtonClickEvent(request))) {
            return;
        }

        if (ButtonClickDispatcher.tryHandle(client, actionButton, actionIndex)) {
            return;
        }

        if (client.activeInterfaceId == 2400) {
            logger.warn(
                    "Unhandled smelting button buttonId={} actionIndex={} iface={} player={}",
                    actionButton,
                    actionIndex,
                    client.activeInterfaceId,
                    client.getPlayerName()
            );
        }

        if (getButtonTraceEnabled() && logger.isTraceEnabled()) {
            logger.trace("Unhandled button buttonId={} player={} iface={}", client.actionButtonId, client.getPlayerName(), client.activeInterfaceId);
        }
    }
}
