package net.dodian.uber.game.netty.listener.in;
import net.dodian.uber.game.api.content.ContentInteraction;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.engine.event.GameEventBus;
import net.dodian.uber.game.events.widget.ButtonClickEvent;
import net.dodian.uber.game.engine.systems.interaction.PlayerTickThrottleService;
import net.dodian.uber.game.ui.buttons.ButtonClickLoggingService;
import net.dodian.uber.game.ui.buttons.ButtonClickRequest;
import net.dodian.uber.game.ui.buttons.InterfaceButtonBinding;
import net.dodian.uber.game.ui.buttons.InterfaceButtonRegistry;
import net.dodian.uber.game.ui.buttons.InterfaceButtonService;
import net.dodian.uber.game.engine.systems.net.PacketButtonService;
import net.dodian.uber.game.engine.systems.dialogue.DialogueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@net.dodian.uber.game.netty.listener.PacketHandler(opcodes = {185, 186})
public class ClickingButtonsListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ClickingButtonsListener.class);
    @Override
    public void handle(Client client, GamePacket packet) {
        int packetSize = packet.size();
        if (packetSize != 2) {
            logger.warn(
                    "Button/key opcode {} expected 2 bytes but received {} from {}",
                    packet.opcode(), packetSize, client.getPlayerName());
            return;
        }

        ByteBuf payload = packet.payload();
        int actionButton = payload.readUnsignedShort();
        int actionIndex = -1;

        if (packet.opcode() == 186) {
            int key = actionButton;
            int optionIndex = -1;
            if (key == 49 || key == 97) optionIndex = 1; // '1' / Numpad 1
            else if (key == 50 || key == 98) optionIndex = 2; // '2' / Numpad 2
            else if (key == 51 || key == 99) optionIndex = 3; // '3' / Numpad 3
            else if (key == 52 || key == 100) optionIndex = 4; // '4' / Numpad 4
            else if (key == 53 || key == 101) optionIndex = 5; // '5' / Numpad 5
            else if (key == 32) { // SPACE
                if (DialogueService.onContinue(client) || DialogueService.onIndexedContinue(client)) {
                    return;
                }
            } else if (key == 27) { // ESCAPE
                client.send(new net.dodian.uber.game.netty.listener.out.RemoveInterfaces());
                return;
            }

            if (optionIndex >= 1 && optionIndex <= 5) {
                int resolvedButton = -1;
                if (client.activeInterfaceId == 2459) { // 2 options
                    if (optionIndex == 1) resolvedButton = 2461;
                    else if (optionIndex == 2) resolvedButton = 2462;
                } else if (client.activeInterfaceId == 2469) { // 3 options
                    if (optionIndex == 1) resolvedButton = 2471;
                    else if (optionIndex == 2) resolvedButton = 2472;
                    else if (optionIndex == 3) resolvedButton = 2473;
                } else if (client.activeInterfaceId == 2480) { // 4 options
                    if (optionIndex == 1) resolvedButton = 2482;
                    else if (optionIndex == 2) resolvedButton = 2483;
                    else if (optionIndex == 3) resolvedButton = 2484;
                    else if (optionIndex == 4) resolvedButton = 2485;
                } else if (client.activeInterfaceId == 2492) { // 5 options
                    if (optionIndex == 1) resolvedButton = 9190;
                    else if (optionIndex == 2) resolvedButton = 9191;
                    else if (optionIndex == 3) resolvedButton = 9192;
                    else if (optionIndex == 4) resolvedButton = 9193;
                    else if (optionIndex == 5) resolvedButton = 9194;
                }

                if (resolvedButton != -1) {
                    actionButton = resolvedButton;
                } else {
                    return;
                }
            } else {
                return;
            }
        }
        if (actionButton == 24115 || actionButton == 24041 || actionButton == 24033 || 
            actionButton == 24048 || actionButton == 24017 || actionButton == 24010 || 
            actionButton == 22845 || actionButton == 24025) {
            
            if (actionButton == 24017 && client.hasStaff()) {
                net.dodian.uber.game.ui.combat.CombatStyleService.refreshWeaponStyleUi(client);
            } else {
                client.autoRetaliate = !client.autoRetaliate;
                client.updateAutoRetaliate();
            }
            return;
        }
        PacketButtonService.recordLastActionIndex(client, actionIndex);

        if (PacketButtonService.isSmeltingInterfaceActive(client)) {
            logger.warn(
                    "Smelting button click buttonId={} actionIndex={} size={} iface={} player={}",
                    actionButton,
                    actionIndex,
                    packetSize,
                    client.activeInterfaceId,
                    client.getPlayerName()
            );
        }

        if (!client.validClient || !ContentInteraction.tryAcquireMs(client, ContentInteraction.BUTTON_GENERAL, 600L)) {
            return;
        }

        PacketButtonService.prepareAction(client, actionButton);

        if (PacketButtonService.tryHandleSkillMulti(client, actionButton)) {
            return;
        }

        if (actionButton >= 36731 && actionButton <= 36769) {
            int index = (actionButton - 36731) / 2;
            if (client.viewingAccountServices) {
                net.dodian.uber.game.ui.AccountServices.handleRowClick(client, index);
            } else {
                handleModcpRowClick(client, index);
            }
            return;
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
            ButtonClickLoggingService.logClick(request, packet.opcode(), true);
            return;
        }

        if (InterfaceButtonService.tryHandle(client, actionButton, actionIndex)) {
            ButtonClickLoggingService.logClick(request, packet.opcode(), true);
            return;
        }

        if (PacketButtonService.isSmeltingInterfaceActive(client)) {
            logger.warn(
                    "Unhandled smelting button buttonId={} actionIndex={} iface={} player={}",
                    actionButton,
                    actionIndex,
                    client.activeInterfaceId,
                    client.getPlayerName()
            );
        }

        ButtonClickLoggingService.logClick(request, packet.opcode(), false);
    }

    private void handleModcpRowClick(Client client, int index) {
        if (client.playerRights < 1) {
            client.sendMessage("You do not have permission to execute this staff command.");
            return;
        }
        if (index < 0 || index >= client.modcpPlayerList.size()) {
            client.sendMessage("No player in this slot.");
            return;
        }
        String targetName = client.modcpPlayerList.get(index);
        client.managingName = targetName;

        // Show player details on the right side of the interface
        Client other = null;
        for (int i = 0; i < net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players.length; i++) {
            net.dodian.uber.game.model.entity.player.Player p = net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players[i];
            if (p != null && p.playerName.equalsIgnoreCase(targetName.trim())) {
                other = (Client) p;
                break;
            }
        }

        client.loadAndShowModcpDetails(targetName, other != null, other != null ? other.connectedFrom : "Offline");

        // Show player options dialogue
        net.dodian.uber.game.social.moderation.ModerationService.beginDialogue(client, targetName);
        client.showPlayerOption(new String[]{
            "Manage " + targetName,
            "Teleport Actions",
            "Moderator Actions",
            "Check Containers",
            "Cancel"
        });
    }
}
